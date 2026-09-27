package com.computer_rescuer.attendance_management.adapter.in.handler;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;

import com.computer_rescuer.attendance_management.adapter.in.exception.InvalidRequestParameterException;
import com.computer_rescuer.attendance_management.adapter.in.model.ApiResponse;
import com.computer_rescuer.attendance_management.adapter.in.model.ResultCode;
import com.computer_rescuer.attendance_management.adapter.out.exception.ExternalIntegrationException;
import com.computer_rescuer.attendance_management.application.port.out.SendErrNoticePort;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

/**
 * {@link GlobalExceptionHandler} の単体テストクラス。
 * <p>
 * コントローラー層で発生する各種例外の捕捉、適切な HTTP ステータスおよび {@link ApiResponse} への変換、
 * 障害通知ポート（{@link SendErrNoticePort}）への連携、および二次障害防止ブロックの命令網羅（C0 100%）を検証します。 Spring コンテキストを起動せず、純粋な
 * Mockito 単体テストとして高速に実行します。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler 単体テスト")
class GlobalExceptionHandlerTest {

  @Mock
  private SendErrNoticePort sendErrNoticePort;

  @InjectMocks
  private GlobalExceptionHandler exceptionHandler;

  @Nested
  @DisplayName("バリデーション例外ハンドリングの検証")
  class ValidationExceptionScenario {

    /**
     * {@link MethodArgumentNotValidException} 発生時に、各フィールドエラーが {@link ApiResponse.Detail} に変換され、HTTP
     * 400 (Bad Request) として返却されることを検証します。
     */
    @Test
    @DisplayName("MethodArgumentNotValidException 発生時、フィールドエラー詳細を含む HTTP 400 レスポンスを返却すること")
    void shouldHandleMethodArgumentNotValidException() {
      // [Given]
      MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
      BindingResult bindingResult = mock(BindingResult.class);

      FieldError fieldError1 = new FieldError("targetObj", "date", "日付形式が不正です");
      FieldError fieldError2 = new FieldError("targetObj", "employeeNumber", "社員番号は必須です");

      given(ex.getBindingResult()).willReturn(bindingResult);
      given(bindingResult.getFieldErrors()).willReturn(List.of(fieldError1, fieldError2));

      // [When]
      ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleValidationException(ex);

      // [Then]
      assertAll(
          () -> assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(),
              "HTTP ステータスは 400 であること"),
          () -> assertNotNull(response.getBody(), "レスポンスボディが存在すること"),
          () -> assertEquals(ResultCode.E_VALIDATION.name(), response.getBody().code(),
              "リザルトコードが E_VALIDATION であること"),
          () -> assertNull(response.getBody().message(),
              "message は Advice で注入されるため null であること"),
          () -> assertNull(response.getBody().data(), "data は null であること"),
          () -> {
            List<ApiResponse.Detail> details = response.getBody().detail();
            assertNotNull(details, "エラー詳細リストが存在すること");
            assertEquals(2, details.size(), "エラー件数が 2 件であること");
            assertEquals("date", details.get(0).key());
            assertEquals("日付形式が不正です", details.get(0).message());
            assertEquals("employeeNumber", details.get(1).key());
            assertEquals("社員番号は必須です", details.get(1).message());
          }
      );
      then(sendErrNoticePort).shouldHaveNoInteractions();
    }

    /**
     * {@link InvalidRequestParameterException} 発生時に、単一のエラー詳細を含む HTTP 400 (Bad Request)
     * が返却されることを検証します。
     */
    @Test
    @DisplayName("InvalidRequestParameterException 発生時、リクエストパラメータエラー詳細を含む HTTP 400 レスポンスを返却すること")
    void shouldHandleInvalidRequestParameterException() {
      // [Given]
      String errorMessage = "開始日は終了日以前である必要があります。";
      InvalidRequestParameterException ex = new InvalidRequestParameterException(errorMessage);

      // [When]
      ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleInvalidRequestParameterException(
          ex);

      // [Then]
      assertAll(
          () -> assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(),
              "HTTP ステータスは 400 であること"),
          () -> assertNotNull(response.getBody(), "レスポンスボディが存在すること"),
          () -> assertEquals(ResultCode.E_VALIDATION.name(), response.getBody().code(),
              "リザルトコードが E_VALIDATION であること"),
          () -> {
            List<ApiResponse.Detail> details = response.getBody().detail();
            assertNotNull(details, "エラー詳細リストが存在すること");
            assertEquals(1, details.size(), "エラー件数が 1 件であること");
            assertEquals("request_parameter", details.get(0).key(),
                "キーが request_parameter であること");
            assertEquals(errorMessage, details.get(0).message(),
                "例外メッセージが格納されていること");
          }
      );
      then(sendErrNoticePort).shouldHaveNoInteractions();
    }
  }

  @Nested
  @DisplayName("外部システム連携例外ハンドリングの検証")
  class ExternalIntegrationExceptionScenario {

    /**
     * {@link ExternalIntegrationException} 発生時に、緊急エラー通知が送信され、 HTTP 503 (Service Unavailable)
     * が返却されることを検証します。
     */
    @Test
    @DisplayName("ExternalIntegrationException 発生時、障害通知を送信し HTTP 503 レスポンスを返却すること")
    void shouldHandleExternalIntegrationExceptionAndSendNotice() {
      // [Given]
      String errorMsg = "HRMOS API トークン取得に失敗しました。";
      ExternalIntegrationException ex = new ExternalIntegrationException(errorMsg);

      // [When]
      ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleExternalIntegrationException(
          ex);

      // [Then]
      assertAll(
          () -> assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode(),
              "HTTP ステータスは 503 であること"),
          () -> assertNotNull(response.getBody(), "レスポンスボディが存在すること"),
          () -> assertEquals(ResultCode.E_EXTERNAL_API.name(), response.getBody().code(),
              "リザルトコードが E_EXTERNAL_API であること"),
          () -> assertNull(response.getBody().detail(), "詳細情報は null であること")
      );

      ArgumentCaptor<String> noticeCaptor = ArgumentCaptor.forClass(String.class);
      then(sendErrNoticePort).should().send(noticeCaptor.capture());

      String capturedMessage = noticeCaptor.getValue();
      assertAll(
          () -> assertTrue(capturedMessage.contains("ステータス: 503 SERVICE_UNAVAILABLE"),
              "ステータスが含まれること"),
          () -> assertTrue(capturedMessage.contains("リザルトコード: E_EXTERNAL_API"),
              "リザルトコードが含まれること"),
          () -> assertTrue(capturedMessage.contains(errorMsg), "エラー原因が含まれること")
      );
    }

    /**
     * 障害通知処理（{@link SendErrNoticePort#send(String)}）自体が例外をスローした場合でも、 catch
     * ブロックで二次障害が抑制され、クライアントへ安全に HTTP 503 レスポンスが返却されることを検証します。
     */
    @Test
    @DisplayName("障害通知送信に失敗した場合でも二次障害を起こさず、HTTP 503 レスポンスを正常に返却すること")
    void shouldHandleExternalIntegrationExceptionEvenWhenNoticeFails() {
      // [Given]
      ExternalIntegrationException ex = new ExternalIntegrationException("通信タイムアウト");
      willThrow(new RuntimeException("通知インフラ障害")).given(sendErrNoticePort)
          .send(anyString());

      // [When]
      ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleExternalIntegrationException(
          ex);

      // [Then]
      assertAll(
          () -> assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode(),
              "二次障害時も HTTP 503 が返却されること"),
          () -> assertNotNull(response.getBody(), "レスポンスボディが存在すること"),
          () -> assertEquals(ResultCode.E_EXTERNAL_API.name(), response.getBody().code(),
              "リザルトコードが維持されること")
      );
    }
  }

  @Nested
  @DisplayName("予期せぬシステム例外ハンドリングの検証")
  class SystemExceptionScenario {

    /**
     * 未捕捉の一般例外（{@link Exception}）発生時に、緊急エラー通知が送信され、 セキュリティのため内部詳細を隠蔽した HTTP 500 (Internal Server
     * Error) が返却されることを検証します。
     */
    @Test
    @DisplayName("予期せぬ Exception 発生時、障害通知を送信し HTTP 500 レスポンスを返却すること")
    void shouldHandleSystemExceptionAndSendNotice() {
      // [Given]
      RuntimeException ex = new NullPointerException("データベース接続の予期せぬ切断");

      // [When]
      ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleSystemException(ex);

      // [Then]
      assertAll(
          () -> assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode(),
              "HTTP ステータスは 500 であること"),
          () -> assertNotNull(response.getBody(), "レスポンスボディが存在すること"),
          () -> assertEquals(ResultCode.E_SYSTEM.name(), response.getBody().code(),
              "リザルトコードが E_SYSTEM であること"),
          () -> assertNull(response.getBody().detail(),
              "セキュリティ保護のため詳細は null であること")
      );

      ArgumentCaptor<String> noticeCaptor = ArgumentCaptor.forClass(String.class);
      then(sendErrNoticePort).should().send(noticeCaptor.capture());

      String capturedMessage = noticeCaptor.getValue();
      assertAll(
          () -> assertTrue(capturedMessage.contains("ステータス: 500 INTERNAL_SERVER_ERROR"),
              "ステータスが含まれること"),
          () -> assertTrue(capturedMessage.contains("リザルトコード: E_SYSTEM"),
              "リザルトコードが含まれること"),
          () -> assertTrue(capturedMessage.contains("NullPointerException"),
              "ルート例外情報が含まれること")
      );
    }

    /**
     * システムエラー通知の送信に失敗した場合でも、catch ブロックで二次障害が抑制され、 安全に HTTP 500 レスポンスが返却されることを検証します。
     */
    @Test
    @DisplayName("システム障害通知の送信に失敗した場合でも二次障害を起こさず、HTTP 500 レスポンスを正常に返却すること")
    void shouldHandleSystemExceptionEvenWhenNoticeFails() {
      // [Given]
      IllegalStateException ex = new IllegalStateException("重大なメモリ不足");
      willThrow(new RuntimeException("メッセージキュー疎通不可")).given(sendErrNoticePort)
          .send(anyString());

      // [When]
      ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleSystemException(ex);

      // [Then]
      assertAll(
          () -> assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode(),
              "二次障害時も HTTP 500 が返却されること"),
          () -> assertNotNull(response.getBody(), "レスポンスボディが存在すること"),
          () -> assertEquals(ResultCode.E_SYSTEM.name(), response.getBody().code(),
              "リザルトコードが維持されること")
      );
    }
  }
}
