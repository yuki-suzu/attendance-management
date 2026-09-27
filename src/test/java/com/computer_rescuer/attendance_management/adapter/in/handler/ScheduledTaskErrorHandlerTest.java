package com.computer_rescuer.attendance_management.adapter.in.handler;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import com.computer_rescuer.attendance_management.application.port.out.SendErrNoticePort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link ScheduledTaskErrorHandler} の単体テストクラス。
 * <p>
 * スケジュールタスク実行中に発生した例外を捕捉し、障害通知ポート（{@link SendErrNoticePort}）へ
 * フォーマットされたメッセージが送信されること、および通知失敗時でもスケジューラースレッドをクラッシュさせない 二次障害防止機構の命令網羅（C0 100%）を検証します。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ScheduledTaskErrorHandler 単体テスト")
class ScheduledTaskErrorHandlerTest {

  @Mock
  private SendErrNoticePort sendErrNoticePort;

  @InjectMocks
  private ScheduledTaskErrorHandler errorHandler;

  @Nested
  @DisplayName("スケジュールタスク例外捕捉シナリオ")
  class HandleErrorScenario {

    /**
     * スケジュールタスクで例外が発生した際、根本原因を含むエラーメッセージが正しく構築され、 通知ポートへ送信されることを検証します。
     */
    @Test
    @DisplayName("タスク例外発生時、フォーマットされた障害通知メッセージを送信すること")
    void shouldSendNoticeWhenTaskThrowsException() {
      // [Given]
      String errorReason = "データベース接続プールが枯渇しました";
      Throwable throwable = new RuntimeException(errorReason);

      // [When]
      errorHandler.handleError(throwable);

      // [Then]
      ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
      then(sendErrNoticePort).should().send(captor.capture());

      String capturedMessage = captor.getValue();
      assertAll(
          () -> assertTrue(capturedMessage.contains("🚨 【バッチ処理エラー】"),
              "ヘッダーが含まれること"),
          () -> assertTrue(capturedMessage.contains(errorReason), "エラー原因が含まれること"),
          () -> assertTrue(capturedMessage.contains("※詳細はサーバーログを確認してください。"),
              "フッターが含まれること")
      );
    }

    /**
     * LINE WORKS への通知処理自体が例外をスローした場合でも、catch ブロックで安全に捕捉され、 スケジューラースレッドへ例外が漏洩（リスロー）しないことを検証します。
     */
    @Test
    @DisplayName("エラー通知送信時に例外が発生しても、二次障害を抑止して例外を外へスローしないこと")
    void shouldNotRethrowWhenNoticePortThrowsException() {
      // [Given]
      Throwable throwable = new IllegalStateException("バッチ処理の異常終了");
      willThrow(new RuntimeException("通知サービスダウン")).given(sendErrNoticePort)
          .send(anyString());

      // [When & Then]
      assertDoesNotThrow(
          () -> errorHandler.handleError(throwable),
          "二次障害防止の try-catch により、いかなる例外も外へスローされないこと"
      );
      then(sendErrNoticePort).should().send(anyString());
    }
  }
}
