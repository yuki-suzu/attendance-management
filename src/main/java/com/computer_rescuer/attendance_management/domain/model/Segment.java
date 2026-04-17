package com.computer_rescuer.attendance_management.domain.model;

import java.time.LocalTime;

/**
 * 勤務区分（出勤、休日、有給など）を表すドメインモデル。
 */
public record Segment(
    Integer id,
    String title,
    String displayTitle,
    Integer status,
    LocalTime startAt,
    LocalTime endAt
) {

  /**
   * 💡 マスタの「状態（status）」に基づき、出勤すべき日であるかを判定します。 1: 勤務, 2: 休日・休暇, 3: その他
   */
  public boolean isWorkingDay() {
    return Integer.valueOf(1).equals(status);
  }
}
