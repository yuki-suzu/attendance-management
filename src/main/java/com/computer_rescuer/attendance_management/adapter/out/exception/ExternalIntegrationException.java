package com.computer_rescuer.attendance_management.adapter.out.exception;

/**
 * 外部システム（HRMOSやLINE WORKSなど）との連携処理において発生した例外を表現するカスタム例外クラス。
 * <p>
 * HTTP通信時のタイムアウト、4xx/5xxエラー、またはAPIからの想定外のレスポンス（必須項目が欠落している等） を検知した場合にスローされます。この例外は
 * {@code GlobalExceptionHandler} によってキャッチされ、 クライアントには安全な形で連携エラー（503 Service
 * Unavailable等）として通知されます。
 * </p>
 */
public class ExternalIntegrationException extends RuntimeException {

  /**
   * エラーメッセージを指定して例外を構築します。
   *
   * @param message 例外の詳細メッセージ（ログ出力用）
   */
  public ExternalIntegrationException(String message) {
    super(message);
  }

  /**
   * エラーメッセージと原因となった例外を指定して例外を構築します。
   *
   * @param message 例外の詳細メッセージ（ログ出力用）
   * @param cause   原因となった例外（スタックトレースの保持用）
   */
  public ExternalIntegrationException(String message, Throwable cause) {
    super(message, cause);
  }
}
