package com.computer_rescuer.attendance_management.adapter.in.model;

/**
 * アプリケーション固有のステータスコード（ビジネスコード）。
 * <p>
 * このEnumの名前自体が、messages.propertiesのキーとして使用されます。 例: SUCCESS -> messages.properties の SUCCESS=...
 * が読み込まれる
 * </p>
 */
public enum ResultCode {
  /**
   * 正常終了
   */
  SUCCESS,

  /**
   * 入力バリデーションエラー
   */
  E_VALIDATION,

  /**
   * システムエラー（予期せぬ例外）
   */
  E_SYSTEM,

  /**
   * 外部システム（HRMOSなど）との連携エラー
   */
  E_EXTERNAL_API
}
