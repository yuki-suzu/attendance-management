package com.computer_rescuer.attendance_management.adapter.out.kafka.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * notification-service の勤怠異常・月次サマリ通知トピックへ送信するイベント DTO。
 * <p>
 * HRMOS 固有の区分ID等を隠蔽し、通知表示に必要な集計結果（予定休・当欠・半休・遅延）を 従業員単位に集約した形式で搬送します。
 * </p>
 *
 * @param procMonth 処理対象月（yyyy-MM）
 * @param employees 勤怠集計結果の従業員リスト
 */
public record AttendanceIrregularityEvent(
    @JsonProperty("proc_month")
    String procMonth,

    @JsonProperty("employees")
    List<EmployeeIrregularitySummary> employees
) {

  /**
   * 従業員ごとの月次勤怠集約情報。
   *
   * @param employeeNumber          社員番号
   * @param departmentName          所属部門名
   * @param fullName                氏名
   * @param scheduledHolidayCount   予定休日数
   * @param unscheduledHolidayCount 当日欠勤日数
   * @param halfHolidayCount        半日休暇日数（午前・午後の合算）
   * @param delayCount              遅延回数
   */
  public record EmployeeIrregularitySummary(
      @JsonProperty("employee_number")
      String employeeNumber,

      @JsonProperty("department_name")
      String departmentName,

      @JsonProperty("full_name")
      String fullName,

      @JsonProperty("scheduled_holiday_count")
      int scheduledHolidayCount,

      @JsonProperty("unscheduled_holiday_count")
      int unscheduledHolidayCount,

      @JsonProperty("half_holiday_count")
      int halfHolidayCount,

      @JsonProperty("delay_count")
      int delayCount
  ) {

  }
}
