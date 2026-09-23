package com.computer_rescuer.attendance_management.application.interactor;

import com.computer_rescuer.attendance_management.adapter.out.hrmos.client.HrmosAuthApi;
import com.computer_rescuer.attendance_management.adapter.out.hrmos.client.HrmosWorkOutputApi;
import com.computer_rescuer.attendance_management.adapter.out.hrmos.model.HrmosDailyWorkOutput;
import com.computer_rescuer.attendance_management.adapter.out.kafka.dto.AttendanceIrregularityEvent;
import com.computer_rescuer.attendance_management.adapter.out.kafka.support.AttendanceIrregularityEventBuilder;
import com.computer_rescuer.attendance_management.application.port.in.MonthlyAttendanceSummaryUseCase;
import com.computer_rescuer.attendance_management.application.port.out.FetchEmployeeDepartmentPort;
import com.computer_rescuer.attendance_management.application.port.out.FetchEmployeePort;
import com.computer_rescuer.attendance_management.application.port.out.MonthlyAttendanceSummaryRepositoryPort;
import com.computer_rescuer.attendance_management.application.port.out.MonthlyAttendanceSummaryRepositoryPort.SummaryItem;
import com.computer_rescuer.attendance_management.application.port.out.NotifyMonthlySummaryPort;
import com.computer_rescuer.attendance_management.application.service.MonthlyAttendanceAggregator;
import com.computer_rescuer.attendance_management.domain.model.Employee;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 月次勤怠サマリ集計ユースケースの実装クラス。
 * <p>
 * 各従業員の勤怠実績取得と集計、DB への差分 UPSERT、および未送信サマリの Kafka 連携フローを統括します。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MonthlyAttendanceSummaryInteractor implements MonthlyAttendanceSummaryUseCase {

  private final HrmosAuthApi authApi;
  private final HrmosWorkOutputApi workOutputApi;
  private final FetchEmployeePort fetchEmployeePort;
  private final FetchEmployeeDepartmentPort fetchEmployeeDepartmentPort;
  private final MonthlyAttendanceSummaryRepositoryPort summaryRepositoryPort;
  private final NotifyMonthlySummaryPort notifyMonthlySummaryPort;
  private final MonthlyAttendanceAggregator aggregator;
  private final AttendanceIrregularityEventBuilder eventBuilder;

  /**
   * {@inheritDoc}
   */
  @Override
  @Transactional(rollbackFor = Exception.class)
  public void execute(YearMonth targetMonth) {
    String procMonth = targetMonth.toString();
    LocalDate today = LocalDate.now();
    log.info("【月次勤怠サマリ】{} の集計処理を開始します（集計上限日: {}）", procMonth, today);

    String token = authApi.fetchToken();
    List<Employee> employees = fetchEmployeePort.fetchAll();
    log.info("対象従業員数: {} 名", employees.size());

    // 1. 各従業員ごとに逐次取得し、集計して DB へ UPSERT（メモリ保護）
    for (Employee emp : employees) {
      try {
        List<HrmosDailyWorkOutput> outputs = workOutputApi.fetchMonthlyWorkOutputsByUser(
            token, procMonth, emp.id()
        );
        List<SummaryItem> items = aggregator.aggregate(outputs, today);
        if (!items.isEmpty()) {
          summaryRepositoryPort.upsertSummaries(procMonth, emp.employeeNumber(), items);
        }
      } catch (Exception e) {
        log.error("従業員 {} ({}) の月次勤怠集計に失敗しました: {}",
            emp.employeeNumber(), emp.id(), e.getMessage(), e);
      }
    }

    // 2. msg_send = '0' が存在する従業員番号を抽出
    List<String> unsentEmpNumbers = summaryRepositoryPort.findEmployeeNumbersWithUnsentSummary(
        procMonth);
    if (unsentEmpNumbers.isEmpty()) {
      log.info("ℹ️ 【月次勤怠サマリ】差分が検知された従業員はいませんでした。対象月: {}", procMonth);
      return;
    }

    log.info("📢 【月次勤怠サマリ】差分が検知された従業員: {} 名。通知イベントを生成します。",
        unsentEmpNumbers.size());

    // 3. 該当従業員の当月サマリ全量を DB から取得
    var persistedSummaries = summaryRepositoryPort.findSummariesByEmployeeNumbers(procMonth,
        unsentEmpNumbers);

    // 4. マスタ情報を補完して Kafka イベントを構築
    Map<String, Employee> empMap = employees.stream()
        .collect(Collectors.toMap(Employee::employeeNumber, e -> e, (e1, e2) -> e1));
    List<Integer> targetUserIds = unsentEmpNumbers.stream()
        .map(empMap::get)
        .filter(java.util.Objects::nonNull)
        .map(Employee::id)
        .toList();
    Map<Integer, String> deptMap = fetchEmployeeDepartmentPort.fetchDepartmentMapByUserIds(
        targetUserIds);

    AttendanceIrregularityEvent event = eventBuilder.buildEvent(
        procMonth, persistedSummaries, empMap, deptMap);

    // 5. Kafka へディスパッチ
    notifyMonthlySummaryPort.sendMonthlySummary(event);

    // 6. 送信完了フラグを更新
    summaryRepositoryPort.markAsSent(procMonth, unsentEmpNumbers);
    log.info("✅ 【月次勤怠サマリ完了】対象月: {}, 送信従業員数: {} 名", procMonth,
        unsentEmpNumbers.size());
  }
}
