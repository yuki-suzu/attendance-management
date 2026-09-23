package com.computer_rescuer.attendance_management.application.interactor;

import com.computer_rescuer.attendance_management.adapter.out.hrmos.mapper.HrmosStampLogMapper;
import com.computer_rescuer.attendance_management.adapter.out.kafka.dto.UnstampedAlertEvent;
import com.computer_rescuer.attendance_management.adapter.out.kafka.dto.UnstampedDirectReminderEvent;
import com.computer_rescuer.attendance_management.adapter.out.kafka.support.UnstampedAlertEventBuilder;
import com.computer_rescuer.attendance_management.application.port.in.NotifyUnstampedAlertUseCase;
import com.computer_rescuer.attendance_management.application.port.out.CheckedEmployeeRepositoryPort;
import com.computer_rescuer.attendance_management.application.port.out.FetchDailyWorkRecordPort;
import com.computer_rescuer.attendance_management.application.port.out.FetchEmployeeByIdPort;
import com.computer_rescuer.attendance_management.application.port.out.FetchEmployeeDepartmentPort;
import com.computer_rescuer.attendance_management.application.port.out.FetchSegmentPort;
import com.computer_rescuer.attendance_management.application.port.out.FetchStampLogPort;
import com.computer_rescuer.attendance_management.application.port.out.NotifyUnstampedAlertPort;
import com.computer_rescuer.attendance_management.domain.model.DailyAttendance;
import com.computer_rescuer.attendance_management.domain.model.DailyAttendance.Status;
import com.computer_rescuer.attendance_management.domain.model.DailyWorkRecord;
import com.computer_rescuer.attendance_management.domain.model.Employee;
import com.computer_rescuer.attendance_management.domain.model.Segment;
import com.computer_rescuer.attendance_management.infrastructure.property.KafkaProperties;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 未打刻アラート通知ユースケースの実装クラス。
 * <p>
 * HRMOS 実績、打刻ログ、ローカルマスタを突き合わせて未打刻者を特定し、 チェック済み履歴（{@code t_checked_employee}）の参照および更新を行いつつ、
 * 出力ポート（{@link NotifyUnstampedAlertPort}）を通じて Kafka へイベントを発行します。
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
  private final FetchEmployeeByIdPort fetchEmployeeByIdPort;
  private final CheckedEmployeeRepositoryPort checkedEmployeeRepositoryPort;
  private final NotifyUnstampedAlertPort notifyUnstampedAlertPort;
  private final UnstampedAlertEventBuilder eventBuilder;
  private final HrmosStampLogMapper stampMapper;
  private final KafkaProperties kafkaProperties;

  /**
   * {@inheritDoc}
   */
  @Override
  @Transactional(rollbackFor = Exception.class)
  public void execute(LocalDate date) {
    ZonedDateTime now = ZonedDateTime.now();

    // 1. 未打刻者の検知・特定
    List<DailyAttendance> alerts = detectUnstampedAttendances(date, now);

    if (alerts.isEmpty()) {
      log.info("未打刻者は検知されませんでした。対象日: {}", date);
      return;
    }

    log.info("未打刻者を {} 名検知しました。対象日: {}", alerts.size(), date);

    List<Integer> userIds = alerts.stream().map(DailyAttendance::userId).toList();
    Map<Integer, Employee> employeeMap = fetchEmployeeByIdPort.fetchEmployeeMapByUserIds(userIds);
    Set<Integer> alreadyCheckedTodayIds = checkedEmployeeRepositoryPort.findCheckedEmployeeIds(date,
        userIds);
    Map<Integer, Integer> monthlyCounts = checkedEmployeeRepositoryPort.countMonthlyChecked(date,
        userIds);

    // 2. 管理者向けアラートイベントの送信
    UnstampedAlertEvent alertEvent = eventBuilder.buildManagerAlertEvent(
        date, now.toLocalDateTime(), alerts, employeeMap, alreadyCheckedTodayIds, monthlyCounts);
    notifyUnstampedAlertPort.sendManagerAlert(alertEvent);

    // 3. 本人向けDMイベントの送信（環境変数フラグおよび本日初回検知者のみ）
    if (kafkaProperties.directReminderEnabled()) {
      UnstampedDirectReminderEvent directEvent = eventBuilder.buildDirectReminderEvent(
          alerts, employeeMap, alreadyCheckedTodayIds);

      if (!directEvent.employees().isEmpty()) {
        notifyUnstampedAlertPort.sendDirectReminder(directEvent);
      } else {
        log.info("ℹ️ 本日分の未打刻DMは全員送信済みのため、スキップしました。");
      }
    } else {
      log.info("ℹ️ 本人向けDM機能は無効化されています (app.kafka.direct-reminder-enabled = false)");
    }

    // 4. チェック済み履歴テーブルへの保存（多重実行時の二重登録は自動無視）
    checkedEmployeeRepositoryPort.saveAll(
        date,
        userIds,
        CheckedEmployeeRepositoryPort.REASON_CODE_UNSTAMPED
    );
  }

  /**
   * 各種データソースを集約・突合し、未打刻対象者のリストを抽出します。
   */
  private List<DailyAttendance> detectUnstampedAttendances(LocalDate date, ZonedDateTime now) {
    var records = fetchDailyWorkRecordPort.fetchByDate(date);
    var stampLogs = fetchStampLogPort.fetchDailyLogs(date);
    var domainSegments = fetchSegmentPort.fetchAll();

    List<Integer> userIds = records.stream().map(DailyWorkRecord::userId).toList();
    Map<Integer, String> departmentMap = fetchEmployeeDepartmentPort.fetchDepartmentMapByUserIds(
        userIds);
    Map<Integer, LocalTime> clockInMap = stampMapper.toClockInMap(stampLogs);

    Map<String, Segment> segmentMap = domainSegments.stream()
        .collect(Collectors.toMap(Segment::title, s -> s, (existing, replacement) -> existing));

    return records.stream()
        .filter(r -> !"0000000000".equals(r.employeeNumber()))
        .map(r -> r.withStampingTime(clockInMap.get(r.userId())))
        .map(r -> r.withDepartmentName(departmentMap.getOrDefault(r.userId(), "未所属")))
        .map(r -> {
          Segment segment = segmentMap.get(r.segmentTitle());
          LocalTime scheduledTime = (segment != null) ? segment.startAt() : null;
          return DailyAttendance.create(r, scheduledTime, date, now);
        })
        .filter(attendance -> attendance.status() == Status.LATE_OR_FORGOT)
        .toList();
  }
}
