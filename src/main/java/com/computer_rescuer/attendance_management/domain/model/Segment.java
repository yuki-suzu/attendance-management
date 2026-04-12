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

}
