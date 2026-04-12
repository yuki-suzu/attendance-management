package com.computer_rescuer.attendance_management.domain.model;

/**
 * システム共通の従業員ドメインモデル。
 * <p>
 * 外部API（HRMOSなど）やデータベースの構造に依存せず、 アプリケーション内で従業員情報を扱うための不変オブジェクト（Record）です。
 * </p>
 *
 * @param id               従業員ID（システム内部ID）
 * @param employeeNumber   社員番号
 * @param lastName         姓
 * @param firstName        名
 * @param email            メールアドレス
 * @param departmentId     所属部門ID
 * @param defaultSegmentId 初期表示の勤務区分ID
 * @param employmentId     雇用形態ID
 */
public record Employee(
    Integer id,
    String employeeNumber,
    String lastName,
    String firstName,
    String email,
    Integer departmentId,
    Integer defaultSegmentId,
    Integer employmentId
) {

}
