package com.computer_rescuer.attendance_management.adapter.out.persistence.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * m_employee テーブルとマッピングされる Spring Data JDBC 用のエンティティ。
 */
@Table("m_employee")
public record EmployeeEntity(
    @Id Integer id, // HRMOSのIDをそのまま主キーとして使う
    String employeeNumber,
    String lastName,
    String firstName,
    String email,
    Integer departmentId,
    Integer defaultSegmentId,
    Integer employmentId
    // ※ created_at, updated_at はDBのデフォルト値とトリガーで自動設定されるため、
    // INSERTのみを行うこのEntityにはあえて含めていません。
) {

}
