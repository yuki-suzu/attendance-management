package com.computer_rescuer.attendance_management.adapter.out.kafka;

import com.computer_rescuer.attendance_management.adapter.out.kafka.dto.UnstampedAlertEvent;
import com.computer_rescuer.attendance_management.adapter.out.kafka.dto.UnstampedDirectReminderEvent;
import com.computer_rescuer.attendance_management.application.port.out.FetchEmployeeByIdPort;
import com.computer_rescuer.attendance_management.application.port.out.NotifyUnstampedAlertPort;
import com.computer_rescuer.attendance_management.domain.model.DailyAttendance;
import com.computer_rescuer.attendance_management.domain.model.Employee;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 未打刻アラート通知出力ポート（{@link NotifyUnstampedAlertPort}）の Kafka 実装アダプター。
 * <p>
 * 未打刻と判定された従業員リストを受け取り、メールアドレス等のマスタ情報を補完した上で、 notification-service が購読する以下の2つの Kafka トピックへイベントを
 * Publish します：<br> 1. 管理者向けサマリーアラートトピック（{@code unstamped-alert-topic}）<br> 2.
 * 従業員本人向け個別DMリマインドトピック（{@code unstamped-direct-topic}）
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaUnstampedAlertAdapter implements NotifyUnstampedAlertPort {

  private final KafkaTemplate<String, Object> kafkaTemplate;
  private final FetchEmployeeByIdPort fetchEmployeeByIdPort;

  @Value("${app.kafka.topics.unstamped-alert:unstamped-alert-topic}")
  private String unstampedAlertTopic;

  @Value("${app.kafka.topics.unstamped-direct:unstamped-direct-topic}")
  private String unstampedDirectTopic;

  /**
   * {@inheritDoc}
   */
  @Override
  public void sendAlert(List<DailyAttendance> alerts) {
    if (alerts == null || alerts.isEmpty()) {
      log.info("ℹ️ 未打刻者が0件のため、Kafka へのイベント発行をスキップします。");
      return;
    }

    try {
      // 1. メールアドレス補完のため、内部IDから Employee マスタを取得
      List<Integer> userIds = alerts.stream().map(DailyAttendance::userId).toList();
      Map<Integer, Employee> employeeMap = fetchEmployeeByIdPort.fetchEmployeeMapByUserIds(userIds);

      LocalDate targetDate = LocalDate.now();
      LocalDateTime now = LocalDateTime.now();

      // 2. 管理者向けイベント（UnstampedAlertEvent）の組み立てと送信
      List<UnstampedAlertEvent.UnstampedEmployee> alertEmployees = alerts.stream()
          .map(a -> {
            Employee emp = employeeMap.get(a.userId());
            String email =
                (emp != null && emp.email() != null) ? emp.email() : "unknown@example.com";
            // TODO: 当月累積回数の算出ロジック実装までは暫定で 1 を設定
            int count = 1;

            return new UnstampedAlertEvent.UnstampedEmployee(
                a.employeeNumber(),
                email,
                a.departmentName(),
                a.fullName(),
                a.scheduledStartAt(),
                count
            );
          })
          .toList();

      UnstampedAlertEvent alertEvent = new UnstampedAlertEvent(targetDate, now, alertEmployees);
      kafkaTemplate.send(unstampedAlertTopic, alertEvent);
      log.info("📢 [Kafka送信] 管理者向け未打刻アラート（{}件）をトピック '{}' へ Publish しました。",
          alertEmployees.size(), unstampedAlertTopic);

      // 3. 本人向けDMイベント（UnstampedDirectReminderEvent）の組み立てと送信
      List<UnstampedDirectReminderEvent.DirectReminderEmployee> directEmployees = alerts.stream()
          .map(a -> {
            Employee emp = employeeMap.get(a.userId());
            String email =
                (emp != null && emp.email() != null) ? emp.email() : "unknown@example.com";

            return new UnstampedDirectReminderEvent.DirectReminderEmployee(
                a.employeeNumber(),
                email,
                a.fullName()
            );
          })
          .toList();

      UnstampedDirectReminderEvent directEvent = new UnstampedDirectReminderEvent(directEmployees);
      // TODO: 初回検知時のみ発行する。その考慮実装まではコメントアウト
//            kafkaTemplate.send(unstampedDirectTopic, directEvent);
      log.info("📢 [Kafka送信] 本人向け未打刻DMリマインド（{}件）をトピック '{}' へ Publish しました。",
          directEmployees.size(), unstampedDirectTopic);

    } catch (Exception e) {
      log.error("❌ [二次障害防止] 未打刻アラートの Kafka Publish に失敗しました: {}",
          e.getMessage(), e);
    }
  }
}
