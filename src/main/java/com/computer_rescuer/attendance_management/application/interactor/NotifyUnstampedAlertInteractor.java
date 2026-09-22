package com.computer_rescuer.attendance_management.application.interactor;

import com.computer_rescuer.attendance_management.adapter.out.hrmos.mapper.HrmosStampLogMapper;
import com.computer_rescuer.attendance_management.application.port.in.NotifyUnstampedAlertUseCase;
import com.computer_rescuer.attendance_management.application.port.out.FetchDailyWorkRecordPort;
import com.computer_rescuer.attendance_management.application.port.out.FetchEmployeeDepartmentPort;
import com.computer_rescuer.attendance_management.application.port.out.FetchSegmentPort;
import com.computer_rescuer.attendance_management.application.port.out.FetchStampLogPort;
import com.computer_rescuer.attendance_management.application.port.out.NotifyUnstampedAlertPort;
import com.computer_rescuer.attendance_management.domain.model.DailyAttendance;
import com.computer_rescuer.attendance_management.domain.model.DailyAttendance.Status;
import com.computer_rescuer.attendance_management.domain.model.DailyWorkRecord;
import com.computer_rescuer.attendance_management.domain.model.Segment;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 未打刻アラート通知ユースケースの実装クラス。
 * <p>
 * HRMOS実績、打刻ログ、ローカル部門マスタ、勤務区分マスタを突き合わせて未打刻者を特定し、 出力ポート（{@link NotifyUnstampedAlertPort}）を通じて Kafka
 * へイベントを発行します。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyUnstampedAlertInteractor implements NotifyUnstampedAlertUseCase {

  private final FetchDailyWorkRecordPort fetchDailyWorkRecordPort;
  private final FetchStampLogPort fetchStampLogPort;
  private final FetchSegmentPort fetchSegmentPort;
  private final FetchEmployeeDepartmentPort fetchEmployeeDepartmentPort;
  private final NotifyUnstampedAlertPort notifyUnstampedAlertPort;
  private final HrmosStampLogMapper stampMapper;

  /**
   * {@inheritDoc}
   */
  @Override
  public void execute(LocalDate date) {
    ZonedDateTime now = ZonedDateTime.now();

    // 1. データソースの集約
    var records = fetchDailyWorkRecordPort.fetchByDate(date);
    var stampLogs = fetchStampLogPort.fetchDailyLogs(date);
    var domainSegments = fetchSegmentPort.fetchAll();

    // 2. ローカルDBからの正確な所属名マップの構築
    List<Integer> userIds = records.stream().map(DailyWorkRecord::userId).toList();
    Map<Integer, String> departmentMap = fetchEmployeeDepartmentPort.fetchDepartmentMapByUserIds(
        userIds);

    // 3. 突合用ルックアップデータの準備（打刻時刻と勤務区分マスタ）
    Map<Integer, LocalTime> clockInMap = stampMapper.toClockInMap(stampLogs);

    Map<String, Segment> segmentMap = domainSegments.stream()
        .collect(Collectors.toMap(
            Segment::title,
            s -> s,
            (existing, replacement) -> existing
        ));

    // 4. マスタ駆動の厳密な判定ロジックとパッチの適用
    var alerts = records.stream()
        .filter(r -> !"0000000000".equals(r.employeeNumber()))
        .map(r -> {
          LocalTime stampingTime = clockInMap.get(r.userId());
          return r.withStampingTime(stampingTime);
        })
        .map(r -> r.withDepartmentName(departmentMap.getOrDefault(r.userId(), "未所属")))
        .map(r -> {
          Segment segment = segmentMap.get(r.segmentTitle());
          // 予定時刻が存在しない（null）区分の場合はそのまま null を渡し NPE を回避
          LocalTime scheduledTime = (segment != null) ? segment.startAt() : null;
          return DailyAttendance.create(r, scheduledTime, date, now);
        })
        .filter(attendance -> attendance.status() == Status.LATE_OR_FORGOT)
        .toList();

    // 5. 通知出力ポートへ委譲（Kafka 経由で notification-service へ連携）
    if (!alerts.isEmpty()) {
      log.info("未打刻者を {} 名検知しました。対象日: {}", alerts.size(), date);
      notifyUnstampedAlertPort.sendAlert(alerts);
    } else {
      log.info("未打刻者は検知されませんでした。対象日: {}", date);
    }
  }
}
