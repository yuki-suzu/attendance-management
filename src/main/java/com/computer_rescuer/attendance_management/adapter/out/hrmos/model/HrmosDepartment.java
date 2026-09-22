package com.computer_rescuer.attendance_management.adapter.out.hrmos.model;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * HRMOS部門取得APIのレスポンス要素
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record HrmosDepartment(
    Integer id,
    String name,
    Integer sequence
) {

}

