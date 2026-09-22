package com.computer_rescuer.attendance_management.adapter.out.kafka.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * notification-service の未打刻者本人向けDMトピックへ送信するイベントDTO。
 *
 * @param employees 個別リマインド送信対象の従業員リスト
 */
public record UnstampedDirectReminderEvent(
    @JsonProperty("employees")
    List<DirectReminderEmployee> employees
) {

  public record DirectReminderEmployee(
      @JsonProperty("employee_number")
      String employeeNumber,

      @JsonProperty("email")
      String email,

      @JsonProperty("full_name")
      String fullName
  ) {

  }
}
