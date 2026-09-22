package com.computer_rescuer.attendance_management.adapter.out.kafka.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * notification-service の未打刻管理者アラートトピックへ送信するイベントDTO。
 *
 * @param targetDate 対象日 (yyyy-MM-dd)
 * @param detectedAt 検知日時
 * @param employees  未打刻従業員リスト
 */
public record UnstampedAlertEvent(
    @JsonProperty("target_date")
    LocalDate targetDate,

    @JsonProperty("detected_at")
    LocalDateTime detectedAt,

    @JsonProperty("employees")
    List<UnstampedEmployee> employees
) {

  public record UnstampedEmployee(
      @JsonProperty("employee_number")
      String employeeNumber,

      @JsonProperty("email")
      String email,

      @JsonProperty("department_name")
      String departmentName,

      @JsonProperty("full_name")
      String fullName,

      @JsonProperty("scheduled_start_at")
      LocalTime scheduledStartAt,

      @JsonProperty("monthly_unstamped_count")
      Integer monthlyUnstampedCount
  ) {

  }
}
