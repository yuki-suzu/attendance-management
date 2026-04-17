package com.computer_rescuer.attendance_management.application.interactor;

import com.computer_rescuer.attendance_management.adapter.out.hrmos.mapper.HrmosStampMapper;
import com.computer_rescuer.attendance_management.application.port.in.NotifyUnstampedAlertUseCase;
import com.computer_rescuer.attendance_management.application.port.out.FetchDailyWorkRecordPort;
import com.computer_rescuer.attendance_management.application.port.out.FetchEmployeeDepartmentPort;
import com.computer_rescuer.attendance_management.application.port.out.FetchHrmosStampPort;
import com.computer_rescuer.attendance_management.application.port.out.FetchSegmentPort;
import com.computer_rescuer.attendance_management.application.port.out.SendAlertPort;
import com.computer_rescuer.attendance_management.application.support.UnstampedAlertMessageFormatter;
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
 * 以下の手順で、外部システムの同期ラグやマスタ情報の不一致を解消し、正確な未打刻検知を実現します。
 * <ol>
 * <li>「日次勤怠(WorkOutputs)」からベースとなる対象者を抽出</li>
 * <li>「打刻ログ(StampLogs)」からリアルタイムの出勤事実を補完</li>
 * <li>ローカルDBの「従業員・部門マスタ」から正確な所属名を補完</li>
 * <li>「勤務区分マスタ(Segment)」のステータスに基づき、出勤義務の有無を厳格に判定</li>
 * </ol>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyUnstampedAlertInteractor implements NotifyUnstampedAlertUseCase {

  private final FetchDailyWorkRecordPort fetchDailyWorkRecordPort;
  private final FetchHrmosStampPort fetchHrmosStampPort;
  private final FetchSegmentPort fetchSegmentPort;
  private final FetchEmployeeDepartmentPort fetchEmployeeDepartmentPort;
  private final SendAlertPort sendAlertPort;
  private final HrmosStampMapper stampMapper;
  private final UnstampedAlertMessageFormatter messageFormatter;

  /**
   * {@inheritDoc}
   * <p>
   * 未打刻アラートの検知・通知フローを制御します。
   * </p>
   */
  @Override
  public void execute(LocalDate date) {
    ZonedDateTime now = ZonedDateTime.now();

    // 1. データソースの集約
    var records = fetchDailyWorkRecordPort.fetchByDate(date);
    var hrmosStamps = fetchHrmosStampPort.fetchByDate(date);
    var domainSegments = fetchSegmentPort.fetchAll();

    // 2. ローカルDBからの正確な所属名マップの構築
    List<Integer> userIds = records.stream().map(DailyWorkRecord::userId).toList();
    Map<Integer, String> departmentMap = fetchEmployeeDepartmentPort.fetchDepartmentMapByUserIds(
        userIds);

    // 3. 突合用ルックアップデータの準備（打刻時刻と勤務区分マスタ）
    Map<Integer, LocalTime> clockInMap = stampMapper.toClockInMap(hrmosStamps);
    Map<String, Segment> segmentMap = domainSegments.stream()
        .collect(Collectors.toMap(
            Segment::title,
            s -> s,
            (existing, replacement) -> existing // タイトル重複時の安全策
        ));

    // 4. マスタ駆動の厳密な判定ロジックとパッチの適用
    var alerts = records.stream()
        .filter(r -> !"0000000000".equals(r.employeeNumber())) // 社長以外
        .filter(r -> {
          // 勤務区分マスタを引き、マスタの status=1(勤務) であるか判定（未知の区分は除外）
          Segment segment = segmentMap.get(r.segmentTitle());
          return segment != null && segment.isWorkingDay();
        })
        .map(r -> r.withStampingTime(clockInMap.get(r.userId()))
            .withDepartmentName(departmentMap.getOrDefault(r.userId(), "未所属")))
        .map(r -> {
          Segment segment = segmentMap.get(r.segmentTitle());
          // 予定時刻が存在しない（null）区分の場合はそのまま null を渡し NPE を回避
          LocalTime scheduledTime = (segment != null) ? segment.startAt() : null;
          return DailyAttendance.create(r, scheduledTime, date, now);
        })
        .filter(attendance -> attendance.status() == Status.LATE_OR_FORGOT)
        .toList();

    // 5. フォーマッターへの委譲と通知の実行
    if (!alerts.isEmpty()) {
      log.info("未打刻者を {} 名検知しました。対象日: {}", alerts.size(), date);

      String message = messageFormatter.format(alerts, date);
      sendAlertPort.send(message);
    } else {
      log.info("未打刻者は検知されませんでした。");
    }
  }
}
