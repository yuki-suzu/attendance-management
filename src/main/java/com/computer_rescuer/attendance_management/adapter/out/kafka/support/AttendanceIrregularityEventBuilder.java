package com.computer_rescuer.attendance_management.adapter.out.kafka.support;

import static com.computer_rescuer.attendance_management.application.service.MonthlyAttendanceAggregator.SEG_AM_HALF_HOLIDAY;
import static com.computer_rescuer.attendance_management.application.service.MonthlyAttendanceAggregator.SEG_DELAY;
import static com.computer_rescuer.attendance_management.application.service.MonthlyAttendanceAggregator.SEG_PM_HALF_HOLIDAY;
import static com.computer_rescuer.attendance_management.application.service.MonthlyAttendanceAggregator.SEG_SCHEDULED_HOLIDAY;
import static com.computer_rescuer.attendance_management.application.service.MonthlyAttendanceAggregator.SEG_UNSCHEDULED_HOLIDAY;

import com.computer_rescuer.attendance_management.adapter.out.kafka.dto.AttendanceIrregularityEvent;
import com.computer_rescuer.attendance_management.adapter.out.kafka.dto.AttendanceIrregularityEvent.EmployeeIrregularitySummary;
import com.computer_rescuer.attendance_management.application.port.out.MonthlyAttendanceSummaryRepositoryPort.PersistedSummary;
import com.computer_rescuer.attendance_management.domain.model.Employee;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * DB から取得した月次サマリ情報を Kafka 送信用イベント DTO へ変換するビルダー。
 */
@Component
public class AttendanceIrregularityEventBuilder {

  /**
   * 永続化サマリレコード群を従業員単位に集約し、通知用イベントを構築します。
   *
   * @param procMonth   処理対象年月
   * @param summaries   対象従業員の当月サマリ全件リスト
   * @param employeeMap 社員番号をキーとする従業員マスタマップ
   * @param deptMap     内部IDをキーとする部門名マップ
   * @return 構築された {@link AttendanceIrregularityEvent}
   */
  public AttendanceIrregularityEvent buildEvent(
      String procMonth,
      List<PersistedSummary> summaries,
      Map<String, Employee> employeeMap,
      Map<Integer, String> deptMap
  ) {
    // 従業員番号ごとにサマリレコードをグループ化
    Map<String, List<PersistedSummary>> groupedByEmp = summaries.stream()
        .collect(Collectors.groupingBy(PersistedSummary::employeeNumber));

    List<EmployeeIrregularitySummary> employeeSummaries = groupedByEmp.entrySet().stream()
        .map(entry -> {
          String empNum = entry.getKey();
          List<PersistedSummary> empRecords = entry.getValue();

          Employee emp = employeeMap.get(empNum);
          String fullName = (emp != null) ? emp.lastName() + " " + emp.firstName() : "不明";
          String dept = (emp != null) ? deptMap.getOrDefault(emp.id(), "未所属") : "未所属";

          int scheduled = 0;
          int unscheduled = 0;
          int half = 0;
          int delay = 0;

          for (PersistedSummary r : empRecords) {
            int segId = r.segmentId();
            int count = r.count();

            if (segId == SEG_SCHEDULED_HOLIDAY) {
              scheduled += count;
            } else if (segId == SEG_UNSCHEDULED_HOLIDAY) {
              unscheduled += count;
            } else if (segId == SEG_AM_HALF_HOLIDAY || segId == SEG_PM_HALF_HOLIDAY) {
              half += count; // 💡 今前半休と今後半休を合算！
            } else if (segId == SEG_DELAY) {
              delay += count;
            }
          }

          return new EmployeeIrregularitySummary(
              empNum,
              dept,
              fullName,
              scheduled,
              unscheduled,
              half,
              delay
          );
        })
        .toList();

    return new AttendanceIrregularityEvent(procMonth, employeeSummaries);
  }
}
