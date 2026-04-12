package com.computer_rescuer.attendance_management.adapter.out.hrmos.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * HRMOSのユーザー情報（従業員マスタ）APIのレスポンス要素
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record HrmosUser(
    Integer id,
    String number,
    String lastName,
    String firstName,
    String email,
    Integer departmentId,
    Integer defaultSegmentId,
    Integer employmentId
) {

}
