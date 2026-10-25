package com.computer_rescuer.attendance_management.adapter.out.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.computer_rescuer.attendance_management.adapter.out.kafka.dto.NotificationCommand;
import com.computer_rescuer.attendance_management.application.port.out.SendErrNoticePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * {@link KafkaErrNoticeAdapter} の単体テストクラス。
 * <p>
 * システムエラー通知出力ポート（{@link SendErrNoticePort}）の Kafka 実装として、 以下の責務と振る舞いを網羅的に検証します:
 * <ul>
 *   <li>エラー通知本文から {@link NotificationCommand} DTO への正確なカプセル化（チャネル種別・宛先設定）</li>
 *   <li>設定されたトピック名への {@link KafkaTemplate#send(String, Object)} ディスパッチ</li>
 *   <li>メッセージングブローカー通信切断・例外発生時における二次障害防止（例外の安全な隠蔽と元処理の保護）</li>
 *   <li>null や空文字などの境界値メッセージに対する堅牢性</li>
 * </ul>
 * Spring コンテキストを起動せず、Mockito と AssertJ を用いた高速かつ決定論的な単体テストを実行します。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("KafkaErrNoticeAdapter 単体テスト")
class KafkaErrNoticeAdapterTest {

  @Mock
  private KafkaTemplate<String, Object> kafkaTemplate;

  @InjectMocks
  private KafkaErrNoticeAdapter adapter;

  private static final String TEST_NOTIFICATION_TOPIC = "test-system-error-topic";

  /**
   * テストの前準備として、{@code @Value} アノテーションで注入される トピック名フィールドをリフレクション経由で初期化します。
   */
  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(adapter, "notificationTopic", TEST_NOTIFICATION_TOPIC);
  }

  @Nested
  @DisplayName("正常系シナリオ")
  class SuccessScenario {

    /**
     * 【テスト観点 / 責務】 正常なエラー通知メッセージを受信した際、システム管理者向け（LINE_WORKS / USER）の {@link NotificationCommand}
     * が構築され、設定された Kafka トピックへ正しく送信されることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>トピック名が "test-system-error-topic" に設定されていること</li>
     *   <li>送信対象のエラーメッセージとして "🚨 【緊急】DB接続プールが枯渇しました" が指定されること</li>
     *   <li>{@link KafkaTemplate#send(String, Object)} が正常に完了すること</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>{@link KafkaTemplate#send(String, Object)} が1回呼び出されること</li>
     *   <li>第1引数に指定のトピック名が渡されていること</li>
     *   <li>第2引数の {@link NotificationCommand} の各フィールド（channelType="LINE_WORKS", destinationType="USER", targetId=null, message=指定文言）が仕様通り構築されていること</li>
     * </ul>
     */
    @Test
    @DisplayName("正常系: エラーメッセージからシステムエラー用コマンドが生成され、指定トピックへPublishされること")
    void shouldPublishNotificationCommandToConfiguredTopic() {
      // [Given]
      String errorMessage = "🚨 【緊急】DB接続プールが枯渇しました";

      // [When]
      adapter.send(errorMessage);

      // [Then]
      ArgumentCaptor<NotificationCommand> commandCaptor = ArgumentCaptor.forClass(
          NotificationCommand.class);
      verify(kafkaTemplate).send(eq(TEST_NOTIFICATION_TOPIC), commandCaptor.capture());

      NotificationCommand capturedCommand = commandCaptor.getValue();
      assertThat(capturedCommand).isNotNull();
      assertThat(capturedCommand.channelType()).isEqualTo("LINE_WORKS");
      assertThat(capturedCommand.destinationType()).isEqualTo("USER");
      assertThat(capturedCommand.targetId()).isNull();
      assertThat(capturedCommand.message()).isEqualTo(errorMessage);
    }
  }

  @Nested
  @DisplayName("異常系および二次障害防止シナリオ")
  class FaultToleranceScenario {

    /**
     * 【テスト観点 / 責務】 Kafka ブローカーダウン等の通信障害発生時における二次障害防止機構（C1 カバレッジ）の検証。
     * {@link KafkaTemplate#send(String, Object)} が例外をスローした場合でも、 内部の catch
     * ブロックで安全に捕捉され、呼び出し元へ例外が伝播（リスロー）しないことを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>送信対象のエラーメッセージとして "重大な障害" が指定されること</li>
     *   <li>{@link KafkaTemplate#send(String, Object)} が {@link RuntimeException}（ブローカー接続失敗）をスローすること</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>呼び出し元スレッドで例外が発生せず、処理が安全に完走すること（{@code assertDoesNotThrow}）</li>
     *   <li>{@link KafkaTemplate#send(String, Object)} 自体は呼び出されていること</li>
     * </ul>
     */
    @Test
    @DisplayName("二次障害防止: Kafka送信で例外が発生しても、エラーを飲み込み外部へ例外をスローしないこと")
    void shouldCatchExceptionAndPreventSecondaryFailureWhenKafkaPublishFails() {
      // [Given]
      String errorMessage = "重大な障害";
      when(kafkaTemplate.send(eq(TEST_NOTIFICATION_TOPIC), any(NotificationCommand.class)))
          .thenThrow(new RuntimeException("Kafka ブローカーへの接続に失敗しました（タイムアウト）"));

      // [When & Then]
      assertDoesNotThrow(
          () -> adapter.send(errorMessage),
          "Kafka 送信エラーが発生しても、元処理の巻き添え終了を防ぐため例外は外へ漏れてはならない"
      );

      verify(kafkaTemplate).send(eq(TEST_NOTIFICATION_TOPIC), any(NotificationCommand.class));
    }
  }

  @Nested
  @DisplayName("境界値シナリオ")
  class BoundaryScenario {

    /**
     * 【テスト観点 / 責務】 メッセージ本文が null、空文字列、または空白文字の場合の境界値検証。 入力値に依存せず安全に {@link NotificationCommand}
     * が生成され、Kafka 送信が実行されることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>メッセージ本文として null、""、" "、"\t" 等が渡されること</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>{@link NullPointerException} 等が発生せず、コマンドの message にそのまま渡されて送信されること</li>
     * </ul>
     */
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("境界値: メッセージが null または空白文字の場合でも安全にコマンドを構築して送信すること")
    void shouldHandleNullOrBlankMessageSafely(String blankMessage) {
      // [When]
      adapter.send(blankMessage);

      // [Then]
      ArgumentCaptor<NotificationCommand> commandCaptor = ArgumentCaptor.forClass(
          NotificationCommand.class);
      verify(kafkaTemplate).send(eq(TEST_NOTIFICATION_TOPIC), commandCaptor.capture());

      NotificationCommand capturedCommand = commandCaptor.getValue();
      assertThat(capturedCommand).isNotNull();
      assertThat(capturedCommand.message()).isEqualTo(blankMessage);
    }
  }
}
