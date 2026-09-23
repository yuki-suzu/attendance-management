package com.computer_rescuer.attendance_management.adapter.out.kafka;

import com.computer_rescuer.attendance_management.adapter.out.kafka.dto.AttendanceIrregularityEvent;
import com.computer_rescuer.attendance_management.application.port.out.NotifyMonthlySummaryPort;
import com.computer_rescuer.attendance_management.infrastructure.property.KafkaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 月次勤怠サマリ通知ポート（{@link NotifyMonthlySummaryPort}）の Kafka 実装アダプター。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaMonthlySummaryAdapter implements NotifyMonthlySummaryPort {

  private final KafkaTemplate<String, Object> kafkaTemplate;
  private final KafkaProperties properties;

  /**
   * {@inheritDoc}
   */
  @Override
  public void sendMonthlySummary(AttendanceIrregularityEvent event) {
    String topic = properties.topics().attendanceIrregularity();
    kafkaTemplate.send(topic, event);
    log.info("📢 [Kafka送信] 月次勤怠サマリ（{}名分）をトピック '{}' へ Publish しました。",
        event.employees().size(), topic);
  }
}
