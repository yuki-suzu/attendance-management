package com.computer_rescuer.attendance_management.domain.model;

/**
 * 組織の部門情報を表すドメインモデル。
 */
public record Department(
    Integer id,
    String name,
    Integer sequence
) {

}
