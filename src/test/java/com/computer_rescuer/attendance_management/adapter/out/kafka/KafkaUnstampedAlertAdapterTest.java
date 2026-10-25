package com.computer_rescuer.attendance_management.adapter.out.kafka;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.computer_rescuer.attendance_management.adapter.out.kafka.dto.UnstampedAlertEvent;
import com.computer_rescuer.attendance_management.adapter.out.kafka.dto.UnstampedAlertEvent.UnstampedEmployee;
import com.computer_rescuer.attendance_management.adapter.out.kafka.dto.UnstampedDirectReminderEvent;
import com.computer_rescuer.attendance_management.adapter.out.kafka.dto.UnstampedDirectReminderEvent.DirectReminderEmployee;
import com.computer_rescuer.attendance_management.infrastructure.property.KafkaProperties;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * {@link KafkaUnstampedAlertAdapter} の単体テストクラス。
 * <p>
 * 未打刻アラート通知出力ポートの実装として、以下の責務と境界条件を検証します:
 * <ul>
 *   <li>管理者向け未打刻アラートトピック（{@code unstampedAlert}）へのイベントディスパッチ</li>
 *   <li>本人向け個別リマインド DM トピック（{@code unstampedDirect}）へのイベントディスパッチ</li>
 *   <li>未打刻従業員リストが空の場合の境界値安全性</li>
 *   <li>メッセージブローカー接続障害時における例外の上位伝播</li>
 * </ul>
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("KafkaUnstampedAlertAdapter 単体テスト")
class KafkaUnstampedAlertAdapterTest {

  @Mock
  private KafkaTemplate<String, Object> kafkaTemplate;

  @Mock
  private KafkaProperties properties;

  @Mock
  private KafkaProperties.Topics topics;

  @InjectMocks
  private KafkaUnstampedAlertAdapter adapter;

  private final String expectedAlertTopic = "unstamped-alert-test-topic";
  private final String expectedDirectTopic = "unstamped-direct-test-topic";

  @Nested
  @DisplayName("管理者向けアラート送信（sendManagerAlert）シナリオ")
  class SendManagerAlertScenario {

    /**
     * 【テスト観点 / 責務】 正常系検証。管理者向け未打刻アラートイベントが渡された際、
     * プロパティからアラートトピック名を解決し、{@link KafkaTemplate#send(String, Object)} が実行されることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>{@code properties.topics().unstampedAlert()} がアラート用トピック名を返却するよう設定</li>
     *   <li>未打刻従業員を 1 名含む {@link UnstampedAlertEvent} を用意</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>管理者アラートトピック名およびイベント DTO を完全一致で {@code kafkaTemplate.send} が実行（1回）されること</li>
     * </ul>
     */
    @Test
    @DisplayName("正常系: 設定されたトピックに対して管理者向け未打刻アラートを正しく Publish すること")
    void shouldPublishManagerAlertEventToConfiguredTopic() {
      // [Given]
      when(properties.topics()).thenReturn(topics);
      when(topics.unstampedAlert()).thenReturn(expectedAlertTopic);

      UnstampedEmployee employee = new UnstampedEmployee(
          "EMP001", "yamada@example.com", "開発部", "山田 太郎",
          LocalTime.of(9, 0), 1
      );
      UnstampedAlertEvent event = new UnstampedAlertEvent(
          LocalDate.of(2026, 4, 20),
          LocalDateTime.of(2026, 4, 20, 10, 0),
          List.of(employee)
      );

      // [When]
      adapter.sendManagerAlert(event);

      // [Then]
      verify(kafkaTemplate).send(expectedAlertTopic, event);
    }

    /**
     * 【テスト観点 / 責務】 境界値検証。未打刻従業員リストが空（0件）のイベント DTO が渡された場合でも、
     * ログ出力（{@code event.employees().size()}）で例外を発生させず安全に処理が完遂することを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>{@code properties.topics().unstampedAlert()} がトピック名を返却するよう設定</li>
     *   <li>{@code employees} が空リストの {@link UnstampedAlertEvent} を用意</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>対象トピックへ空イベントがディスパッチされること</li>
     * </ul>
     */
    @Test
    @DisplayName("境界値: 従業員リストが空の場合でも、安全に管理者アラートを Publish すること")
    void shouldPublishManagerAlertGracefullyWhenEmployeesListIsEmpty() {
      // [Given]
      when(properties.topics()).thenReturn(topics);
      when(topics.unstampedAlert()).thenReturn(expectedAlertTopic);

      UnstampedAlertEvent emptyEvent = new UnstampedAlertEvent(
          LocalDate.of(2026, 4, 20),
          LocalDateTime.of(2026, 4, 20, 10, 0),
          Collections.emptyList()
      );

      // [When]
      adapter.sendManagerAlert(emptyEvent);

      // [Then]
      verify(kafkaTemplate).send(expectedAlertTopic, emptyEvent);
    }

    /**
     * 【テスト観点 / 責務】 異常系検証。ブローカー通信エラー等の例外が発生した場合に、上位へ例外を伝播させることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>{@code properties.topics().unstampedAlert()} がトピック名を返却するよう設定</li>
     *   <li>{@code kafkaTemplate.send} が {@link KafkaException} をスローするよう設定</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>{@link KafkaException} がそのままスローされること</li>
     * </ul>
     */
    @Test
    @DisplayName("異常系: 管理者アラート送信時に障害が発生した場合、例外を上位へ伝播させること")
    void shouldPropagateExceptionWhenManagerAlertPublishFails() {
      // [Given]
      when(properties.topics()).thenReturn(topics);
      when(topics.unstampedAlert()).thenReturn(expectedAlertTopic);

      UnstampedAlertEvent event = new UnstampedAlertEvent(
          LocalDate.of(2026, 4, 20),
          LocalDateTime.of(2026, 4, 20, 10, 0),
          Collections.emptyList()
      );

      when(kafkaTemplate.send(expectedAlertTopic, event))
          .thenThrow(new KafkaException("Kafka connection refused"));

      // [When & Then]
      assertThatThrownBy(() -> adapter.sendManagerAlert(event))
          .isInstanceOf(KafkaException.class)
          .hasMessage("Kafka connection refused");

      verify(kafkaTemplate).send(expectedAlertTopic, event);
    }
  }

  @Nested
  @DisplayName("本人向け個別リマインド送信（sendDirectReminder）シナリオ")
  class SendDirectReminderScenario {

    /**
     * 【テスト観点 / 責務】 正常系検証。本人向け未打刻ダイレクトリマインドイベントが渡された際、 プロパティから DM
     * トピック名を解決し、{@link KafkaTemplate#send(String, Object)} が実行されることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>{@code properties.topics().unstampedDirect()} が DM 用トピック名を返却するよう設定</li>
     *   <li>リマインド対象従業員を 1 名含む {@link UnstampedDirectReminderEvent} を用意</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>本人DMトピック名およびイベント DTO を完全一致で {@code kafkaTemplate.send} が実行（1回）されること</li>
     * </ul>
     */
    @Test
    @DisplayName("正常系: 設定されたトピックに対して本人向け個別リマインドDMを正しく Publish すること")
    void shouldPublishDirectReminderEventToConfiguredTopic() {
      // [Given]
      when(properties.topics()).thenReturn(topics);
      when(topics.unstampedDirect()).thenReturn(expectedDirectTopic);

      DirectReminderEmployee employee = new DirectReminderEmployee(
          "EMP001", "yamada@example.com", "山田 太郎"
      );
      UnstampedDirectReminderEvent event = new UnstampedDirectReminderEvent(List.of(employee));

      // [When]
      adapter.sendDirectReminder(event);

      // [Then]
      verify(kafkaTemplate).send(expectedDirectTopic, event);
    }

    /**
     * 【テスト観点 / 責務】 境界値検証。リマインド対象従業員が 0 件のイベント DTO が渡された場合でも、 安全に送信が完遂されることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>{@code properties.topics().unstampedDirect()} がトピック名を返却するよう設定</li>
     *   <li>{@code employees} が空リストの {@link UnstampedDirectReminderEvent} を用意</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>対象トピックへ空イベントがディスパッチされること</li>
     * </ul>
     */
    @Test
    @DisplayName("境界値: リマインド対象従業員が空の場合でも、安全に Publish 処理が完遂すること")
    void shouldPublishDirectReminderGracefullyWhenEmployeesListIsEmpty() {
      // [Given]
      when(properties.topics()).thenReturn(topics);
      when(topics.unstampedDirect()).thenReturn(expectedDirectTopic);

      UnstampedDirectReminderEvent emptyEvent = new UnstampedDirectReminderEvent(
          Collections.emptyList()
      );

      // [When]
      adapter.sendDirectReminder(emptyEvent);

      // [Then]
      verify(kafkaTemplate).send(expectedDirectTopic, emptyEvent);
    }

    /**
     * 【テスト観点 / 責務】 異常系検証。DM リマインド送信時に Kafka ブローカー通信障害等が発生した場合、 例外を握り潰さず上位へ伝播させることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>{@code properties.topics().unstampedDirect()} がトピック名を返却するよう設定</li>
     *   <li>{@code kafkaTemplate.send} が {@link KafkaException} をスローするよう設定</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>{@link KafkaException} がそのままスローされること</li>
     * </ul>
     */
    @Test
    @DisplayName("異常系: 本人DM送信時に障害が発生した場合、例外を上位へ伝播させること")
    void shouldPropagateExceptionWhenDirectReminderPublishFails() {
      // [Given]
      when(properties.topics()).thenReturn(topics);
      when(topics.unstampedDirect()).thenReturn(expectedDirectTopic);

      UnstampedDirectReminderEvent event = new UnstampedDirectReminderEvent(
          Collections.emptyList()
      );

      when(kafkaTemplate.send(expectedDirectTopic, event))
          .thenThrow(new KafkaException("Kafka produce failed"));

      // [When & Then]
      assertThatThrownBy(() -> adapter.sendDirectReminder(event))
          .isInstanceOf(KafkaException.class)
          .hasMessage("Kafka produce failed");

      verify(kafkaTemplate).send(expectedDirectTopic, event);
    }
  }
}
