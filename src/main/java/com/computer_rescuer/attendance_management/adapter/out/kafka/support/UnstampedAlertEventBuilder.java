package com.computer_rescuer.attendance_management.adapter.out.kafka.support;

import com.computer_rescuer.attendance_management.adapter.out.kafka.dto.UnstampedAlertEvent;
import com.computer_rescuer.attendance_management.adapter.out.kafka.dto.UnstampedDirectReminderEvent;
import com.computer_rescuer.attendance_management.domain.model.DailyAttendance;
import com.computer_rescuer.attendance_management.domain.model.Employee;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * 未打刻アラート判定結果から各種 Kafka 送信用イベント DTO を構築するビルダー。
 */
@Component
public class UnstampedAlertEventBuilder {

  /**
   * 未打刻者一覧から管理者向けサマリーアラートイベントを組み立てます。
   *
   * @param targetDate             対象日
   * @param now                    検知日時
   * @param alerts                 未打刻と判定された勤怠リスト
   * @param employeeMap            従業員マスタマップ
   * @param alreadyCheckedTodayIds 本日すでにチェック済みの従業員IDセット
   * @param monthlyCounts          当月の累積チェック回数マップ
   * @return 構築された {@link UnstampedAlertEvent}
   */
  public UnstampedAlertEvent buildManagerAlertEvent(
      LocalDate targetDate,
      LocalDateTime now,
      List<DailyAttendance> alerts,
      Map<Integer, Employee> employeeMap,
      Set<Integer> alreadyCheckedTodayIds,
      Map<Integer, Integer> monthlyCounts
  ) {
    List<UnstampedAlertEvent.UnstampedEmployee> employees = alerts.stream()
        .map(a -> {
          Employee emp = employeeMap.get(a.userId());
          String email = (emp != null && emp.email() != null) ? emp.email() : "unknown@example.com";

          int baseCount = monthlyCounts.getOrDefault(a.userId(), 0);
          int currentCount =
              alreadyCheckedTodayIds.contains(a.userId()) ? Math.max(baseCount, 1) : baseCount + 1;

          return new UnstampedAlertEvent.UnstampedEmployee(
              a.employeeNumber(),
              email,
              a.departmentName(),
              a.fullName(),
              a.scheduledStartAt(),
              currentCount
          );
        })
        .toList();

    return new UnstampedAlertEvent(targetDate, now, employees);
  }

  /**
   * 未打刻者のうち、本日初回検知者のみを抽出して本人向け個別DMイベントを組み立てます。
   *
   * @param alerts                 未打刻と判定された勤怠リスト
   * @param employeeMap            従業員マスタマップ
   * @param alreadyCheckedTodayIds 本日すでにチェック済みの従業員IDセット
   * @return 構築された {@link UnstampedDirectReminderEvent}
   */
  public UnstampedDirectReminderEvent buildDirectReminderEvent(
      List<DailyAttendance> alerts,
      Map<Integer, Employee> employeeMap,
      Set<Integer> alreadyCheckedTodayIds
  ) {
    List<UnstampedDirectReminderEvent.DirectReminderEmployee> directEmployees = alerts.stream()
        .filter(a -> !alreadyCheckedTodayIds.contains(a.userId()))
        .map(a -> {
          Employee emp = employeeMap.get(a.userId());
          String email = (emp != null && emp.email() != null) ? emp.email() : "unknown@example.com";
          return new UnstampedDirectReminderEvent.DirectReminderEmployee(
              a.employeeNumber(),
              email,
              a.fullName()
          );
        })
        .toList();

    return new UnstampedDirectReminderEvent(directEmployees);
  }
}
