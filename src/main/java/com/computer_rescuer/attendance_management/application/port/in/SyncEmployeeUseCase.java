package com.computer_rescuer.attendance_management.application.port.in;

/**
 * 従業員マスター情報を同期するための入力ポート（ユースケース）。
 */
public interface SyncEmployeeUseCase {

  /**
   * 外部システムから最新の従業員情報を取得し、システム内のマスターデータを洗い替えます。
   */
  void syncEmployees();
}
