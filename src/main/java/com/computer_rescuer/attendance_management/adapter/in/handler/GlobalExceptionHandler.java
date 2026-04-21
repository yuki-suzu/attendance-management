package com.computer_rescuer.attendance_management.adapter.in.handler;

import com.computer_rescuer.attendance_management.adapter.in.exception.InvalidRequestParameterException;
import com.computer_rescuer.attendance_management.adapter.in.model.ApiResponse;
import com.computer_rescuer.attendance_management.adapter.in.model.ResultCode;
import com.computer_rescuer.attendance_management.adapter.out.exception.ExternalIntegrationException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * アプリケーション全体で発生する例外を横断的に捕捉し、 クライアントに対して統一されたフォーマット（{@link ApiResponse}）でエラーレスポンスを返却するグローバル例外ハンドラー。
 * <p>
 * 実際のメッセージ文言の解決・注入は {@code ApiResponseAdvice} によって処理されるため、 ここでは適切な {@link ResultCode}
 * と詳細情報（詳細がある場合のみ）をセットして返却することに専念します。
 * </p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  /**
   * コントローラーの引数に対するバリデーション（@Valid / @Validated）で違反があった場合のハンドリングを行います。
   *
   * @param ex 発生したバリデーション例外
   * @return HTTP 400 (Bad Request) と、バリデーションエラーの詳細情報を含むレスポンス
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Void>> handleValidationException(
      MethodArgumentNotValidException ex) {
    log.warn("バリデーションエラーが発生しました: {}", ex.getMessage());

    // SpringのBindingResultからエラー項目とデフォルトメッセージを抽出
    List<ApiResponse.Detail> details = ex.getBindingResult().getFieldErrors().stream()
        .map(error -> new ApiResponse.Detail(error.getField(), error.getDefaultMessage()))
        .toList();

    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiResponse.error(ResultCode.E_VALIDATION, details));
  }

  /**
   * クライアントからのリクエストパラメータに論理的な矛盾や違反があった場合のハンドリングを行います。
   *
   * @param ex 発生したリクエストパラメータ例外
   * @return HTTP 400 (Bad Request) と、エラーメッセージを含むレスポンス
   */
  @ExceptionHandler(InvalidRequestParameterException.class)
  public ResponseEntity<ApiResponse<Void>> handleInvalidRequestParameterException(
      InvalidRequestParameterException ex) {
    log.warn("リクエストパラメータが不正です: {}", ex.getMessage());

    // 💡 詳細情報としてエラーメッセージをセット（単一のエラーとして扱う）
    List<ApiResponse.Detail> details = List.of(
        new ApiResponse.Detail("request_parameter", ex.getMessage()));

    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        // ※ 既存の E_VALIDATION を使うか、必要に応じて E_BAD_REQUEST などを ResultCode に追加してください
        .body(ApiResponse.error(ResultCode.E_VALIDATION, details));
  }

  /**
   * 外部システム（HRMOS等）との連携中にエラーが発生した場合のハンドリングを行います。
   * <p>
   * システム自体のバグではなく、外部要因による一時的なサービス提供不可状態として扱います。
   * </p>
   *
   * @param ex 発生した外部連携例外
   * @return HTTP 503 (Service Unavailable) と、外部連携エラーを示すレスポンス
   */
  @ExceptionHandler(ExternalIntegrationException.class)
  public ResponseEntity<ApiResponse<Void>> handleExternalIntegrationException(
      ExternalIntegrationException ex) {
    log.error("外部システム連携エラーが発生しました: {}", ex.getMessage(), ex);

    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(ApiResponse.error(ResultCode.E_EXTERNAL_API, null));
  }

  /**
   * 上記の専用ハンドラーで捕捉されなかった、予期せぬシステム例外のハンドリングを行います。
   * <p>
   * セキュリティの観点から、スタックトレース等の内部情報はクライアントに返却せず、 固定のシステムエラーコードのみを返却します。
   * </p>
   *
   * @param ex 発生した予期せぬ例外
   * @return HTTP 500 (Internal Server Error) と、システムエラーを示すレスポンス
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleSystemException(Exception ex) {
    log.error("予期せぬシステムエラーが発生しました", ex);

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiResponse.error(ResultCode.E_SYSTEM, null));
  }
}
