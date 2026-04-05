package com.computer_rescuer.attendance_management.adapter.out.hrmos.model;

/**
 * HRMOSのユーザー情報（従業員マスタ）APIのレスポンス要素
 */
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
