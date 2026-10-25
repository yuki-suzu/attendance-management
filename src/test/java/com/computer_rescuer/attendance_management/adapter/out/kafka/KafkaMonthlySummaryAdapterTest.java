package com.computer_rescuer.attendance_management.adapter.out.kafka;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.computer_rescuer.attendance_management.adapter.out.kafka.dto.AttendanceIrregularityEvent;
import com.computer_rescuer.attendance_management.adapter.out.kafka.dto.AttendanceIrregularityEvent.EmployeeIrregularitySummary;
import com.computer_rescuer.attendance_management.infrastructure.property.KafkaProperties;
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
 * {@link KafkaMonthlySummaryAdapter} の単体テストクラス。
 * <p>
 * 月次勤怠サマリ（勤怠異常検知）通知ポートの実装として、以下の責務と境界条件を検証します:
 * <ul>
 *   <li>構成プロパティ（{@link KafkaProperties}）から動的に設定されたトピック名の解決</li>
 *   <li>{@link KafkaTemplate} を介したイベント DTO ペイロードの正常な非同期ディスパッチ</li>
 *   <li>集計対象従業員が 0 件（空リスト）の場合の境界値安全性</li>
 *   <li>メッセージブローカー接続障害時における例外の上位伝播（上位トランザクションロールバック保証）</li>
 * </ul>
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("KafkaMonthlySummaryAdapter 単体テスト")
class KafkaMonthlySummaryAdapterTest {

  @Mock
  private KafkaTemplate<String, Object> kafkaTemplate;

  @Mock
  private KafkaProperties properties;

  @Mock
  private KafkaProperties.Topics topics;

  @InjectMocks
  private KafkaMonthlySummaryAdapter adapter;

  private final String expectedTopic = "attendance-irregularity-test-topic";

  @Nested
  @DisplayName("月次サマリイベント送信（sendMonthlySummary）シナリオ")
  class SendMonthlySummaryScenario {

    /**
     * 【テスト観点 / 責務】 正常系検証。月次サマリイベントが渡された際、プロパティから勤怠異常トピック名を解決し、 イベント DTO をペイロードとして
     * {@link KafkaTemplate#send(String, Object)} が呼び出されることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>{@code properties.topics().attendanceIrregularity()} がテスト用トピック名を返却するよう設定</li>
     *   <li>従業員集約サマリを 1 件含む {@link AttendanceIrregularityEvent} を用意</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>対象トピック名およびイベント DTO を完全な引数として {@code kafkaTemplate.send} が実行（1回）されること</li>
     * </ul>
     */
    @Test
    @DisplayName("正常系: 設定されたトピック名に対して月次勤怠サマリエントを正しく Publish すること")
    void shouldPublishMonthlySummaryEventToConfiguredTopic() {
      // [Given]
      when(properties.topics()).thenReturn(topics);
      when(topics.attendanceIrregularity()).thenReturn(expectedTopic);

      EmployeeIrregularitySummary summary = new EmployeeIrregularitySummary(
          "EMP001", "システム部", "山田 太郎", 8, 1, 2, 0
      );
      AttendanceIrregularityEvent event = new AttendanceIrregularityEvent(
          "2026-04", List.of(summary)
      );

      // [When]
      adapter.sendMonthlySummary(event);

      // [Then]
      verify(kafkaTemplate).send(expectedTopic, event);
    }

    /**
     * 【テスト観点 / 責務】 境界値検証。従業員集約リストが空（0件）のイベント DTO が渡された場合でも、
     * ログ出力（{@code event.employees().size()}）や送信処理で例外を発生させず安全に送信できることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>{@code properties.topics().attendanceIrregularity()} がトピック名を返却するよう設定</li>
     *   <li>{@code employees} が空リストの {@link AttendanceIrregularityEvent} を用意</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>例外が発生せず、対象トピックへ空イベントがディスパッチされること</li>
     * </ul>
     */
    @Test
    @DisplayName("境界値: 従業員リストが空の場合でも、例外なく安全に Publish 処理が完遂すること")
    void shouldPublishEventGracefullyWhenEmployeesListIsEmpty() {
      // [Given]
      when(properties.topics()).thenReturn(topics);
      when(topics.attendanceIrregularity()).thenReturn(expectedTopic);

      AttendanceIrregularityEvent emptyEvent = new AttendanceIrregularityEvent(
          "2026-04", Collections.emptyList()
      );

      // [When]
      adapter.sendMonthlySummary(emptyEvent);

      // [Then]
      verify(kafkaTemplate).send(expectedTopic, emptyEvent);
    }

    /**
     * 【テスト観点 / 責務】 異常系検証。Kafka ブローカーの切断等により {@link KafkaTemplate#send} が例外をスローした場合、
     * 例外を握り潰さず上位（呼び出し元の Interactor）へ伝播させ、トランザクションのロールバックを誘発することを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>{@code properties.topics().attendanceIrregularity()} がトピック名を返却するよう設定</li>
     *   <li>{@code kafkaTemplate.send} が {@link KafkaException} をスローするようモック設定</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>{@link KafkaException} がそのまま上位へスローされること</li>
     * </ul>
     */
    @Test
    @DisplayName("異常系: Kafkaブローカー障害発生時、例外を握り潰さず上位へ伝播させること")
    void shouldPropagateExceptionWhenKafkaTemplateThrowsException() {
      // [Given]
      when(properties.topics()).thenReturn(topics);
      when(topics.attendanceIrregularity()).thenReturn(expectedTopic);

      AttendanceIrregularityEvent event = new AttendanceIrregularityEvent(
          "2026-04", Collections.emptyList()
      );

      when(kafkaTemplate.send(expectedTopic, event))
          .thenThrow(new KafkaException("Kafka broker is unreachable"));

      // [When & Then]
      assertThatThrownBy(() -> adapter.sendMonthlySummary(event))
          .isInstanceOf(KafkaException.class)
          .hasMessage("Kafka broker is unreachable");

      verify(kafkaTemplate).send(expectedTopic, event);
    }
  }
}
