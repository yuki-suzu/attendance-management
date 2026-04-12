package com.computer_rescuer.attendance_management.adapter.out.hrmos.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.OffsetDateTime;

/**
 * HRMOS勤務区分取得APIのレスポンス要素
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record HrmosSegment(
    Integer id,
    String title,
    String displayTitle,
    Integer status,
    OffsetDateTime startAt,
    OffsetDateTime endAt
) {

}
