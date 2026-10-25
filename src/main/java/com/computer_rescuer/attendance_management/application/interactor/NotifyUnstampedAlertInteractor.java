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
import com.computer_rescuer.attendance_management.domain.service.DailyAttendanceFactory;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 未打刻アラート通知ユースケースの実装クラス（Interactor）。
 * <p>
 * HRMOS の日次勤怠実績、打刻ログ、ローカルマスタを突き合わせて未打刻者を特定し、 チェック済み履歴（{@code t_checked_employee}）の参照・更新を行いつつ、
 * 出力ポート（{@link NotifyUnstampedAlertPort}）を通じて非同期イベントを発行します。<br> 日時取得には {@link Clock}
 * を使用し、判定オブジェクトの組み立ては {@link DailyAttendanceFactory} へ委譲することで、
 * ユースケースオーケストレーションへの関心分離と高いテスト容易性を両立しています。
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
  private final DailyAttendanceFactory dailyAttendanceFactory;
  private final Clock clock;

  /**
   * {@inheritDoc}
   */
  @Override
  @Transactional(rollbackFor = Exception.class)
  public void execute(LocalDate date) {
    ZonedDateTime now = ZonedDateTime.now(clock);

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

    // 3. 本人向けDMイベント（または管理者代理通知イベント）の送信
    UnstampedDirectReminderEvent directEvent = eventBuilder.buildDirectReminderEvent(
        alerts,
        employeeMap,
        alreadyCheckedTodayIds
    );

    if (!directEvent.employees().isEmpty()) {
      notifyUnstampedAlertPort.sendDirectReminder(directEvent);
    } else {
      log.info("ℹ️ 本日分の未打刻DMは全員送信済みのため、スキップしました。");
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
   *
   * @param date 勤怠判定対象日
   * @param now  判定基準日時
   * @return 未打刻と判定された勤怠結果リスト
   */
  private List<DailyAttendance> detectUnstampedAttendances(LocalDate date, ZonedDateTime now) {
    var records = fetchDailyWorkRecordPort.fetchByDate(date);
    var stampLogs = fetchStampLogPort.fetchDailyLogs(date);
    var domainSegments = fetchSegmentPort.fetchAll();

    List<Integer> userIds = records.stream().map(DailyWorkRecord::userId).toList();
    Map<Integer, String> departmentMap = fetchEmployeeDepartmentPort.fetchDepartmentMapByUserIds(
        userIds);
    Map<Integer, LocalTime> clockInMap = stampMapper.toClockInMap(stampLogs);

    // ID昇順で決定論的にソートし、重複時は先勝ち＋エラーログ出力
    Map<String, Segment> segmentMap = domainSegments.stream()
        .sorted(Comparator.comparing(Segment::id))
        .collect(Collectors.toMap(
            Segment::title,
            Function.identity(),
            (existing, replacement) -> {
              log.error(
                  "🚨 【マスタ重複警告】勤務区分名 '{}' が重複しています！ (採用ID: {}, 無視ID: {})",
                  existing.title(), existing.id(), replacement.id());
              return existing;
            }
        ));

    return records.stream()
        .filter(r -> !"0000000000".equals(r.employeeNumber()))
        .map(
            r -> dailyAttendanceFactory.create(r, segmentMap, departmentMap, clockInMap, date, now))
        .filter(attendance -> attendance.status() == Status.LATE_OR_FORGOT)
        .toList();
  }
}
