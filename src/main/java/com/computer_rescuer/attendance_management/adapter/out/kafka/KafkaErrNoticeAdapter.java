package com.computer_rescuer.attendance_management.adapter.out.kafka;

import com.computer_rescuer.attendance_management.adapter.out.kafka.dto.NotificationCommand;
import com.computer_rescuer.attendance_management.application.port.out.SendErrNoticePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * システムエラー通知出力ポート（{@link SendErrNoticePort}）の Kafka 送信実装アダプター。
 * <p>
 * バッチ障害や未捕捉例外の発生時、notification-service の汎用通知トピックへ {@link NotificationCommand} を非同期 Publish します。<br>
 * メッセージングブローカーの通信障害によって元処理が巻き添え終了（二次障害）しないよう、 送信処理は安全に保護されます。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaErrNoticeAdapter implements SendErrNoticePort {

  private final KafkaTemplate<String, Object> kafkaTemplate;

  @Value("${app.kafka.topics.notification:notification-topic}")
  private String notificationTopic;

  /**
   * システムエラー通知メッセージを Kafka トピックへ Publish します。
   *
   * @param message 送信対象のエラー本文
   */
  @Override
  public void send(String message) {
    try {
      log.info("📢 [Kafka送信] システム管理者向けエラー通知をトピック '{}' へ Publish します。",
          notificationTopic);

      NotificationCommand command = NotificationCommand.ofSystemError(message);
      kafkaTemplate.send(notificationTopic, command);

      log.info("✅ [Kafka送信完了] エラー通知メッセージを送信しました。");
    } catch (Exception e) {
      // Kafka が落ちていても元のバッチや例外ハンドリングを共倒れさせないための防護壁
      log.error("❌ [二次障害防止] Kafka へのエラー通知 Publish に失敗しました: {}", e.getMessage(),
          e);
    }
  }
}
