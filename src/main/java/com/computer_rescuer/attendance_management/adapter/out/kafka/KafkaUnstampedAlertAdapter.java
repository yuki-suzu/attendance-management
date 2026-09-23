package com.computer_rescuer.attendance_management.adapter.out.kafka;

import com.computer_rescuer.attendance_management.adapter.out.kafka.dto.UnstampedAlertEvent;
import com.computer_rescuer.attendance_management.adapter.out.kafka.dto.UnstampedDirectReminderEvent;
import com.computer_rescuer.attendance_management.application.port.out.NotifyUnstampedAlertPort;
import com.computer_rescuer.attendance_management.infrastructure.property.KafkaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 未打刻アラート通知出力ポート（{@link NotifyUnstampedAlertPort}）の Kafka 実装アダプター。
 * <p>
 * アプリケーション層から受け取った完成済みのイベント DTO を、 指定された Kafka トピックへ非同期 Publish することのみを単一の責務とします。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaUnstampedAlertAdapter implements NotifyUnstampedAlertPort {

  private final KafkaTemplate<String, Object> kafkaTemplate;
  private final KafkaProperties properties;

  /**
   * {@inheritDoc}
   */
  @Override
  public void sendManagerAlert(UnstampedAlertEvent event) {
    String topic = properties.topics().unstampedAlert();
    kafkaTemplate.send(topic, event);
    log.info("📢 [Kafka送信] 管理者向け未打刻アラート（{}件）をトピック '{}' へ Publish しました。",
        event.employees().size(), topic);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void sendDirectReminder(UnstampedDirectReminderEvent event) {
    String topic = properties.topics().unstampedDirect();
    kafkaTemplate.send(topic, event);
    log.info("📢 [Kafka送信] 本人向け未打刻DMリマインド（{}件）をトピック '{}' へ Publish しました。",
        event.employees().size(), topic);
  }
}
