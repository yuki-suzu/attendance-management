package com.computer_rescuer.attendance_management.infrastructure.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Kafka メッセージングおよびトピック定義に関する構成プロパティレコード。
 * <p>
 * {@code application.yaml} の {@code app.kafka} プレフィックスにバインドされ、
 * アプリケーション全体で使用するトピック名や機能トグル（本人DM配信有無など）を一元管理します。
 * </p>
 *
 * @param directReminderEnabled 本人向け未打刻リマインドDMの送信を有効化するか否かのフラグ
 * @param topics                送受信対象となる Kafka トピック名の定義グループ
 */
@ConfigurationProperties(prefix = "app.kafka")
public record KafkaProperties(
    boolean directReminderEnabled,
    Topics topics
) {

  /**
   * トピック名のグループ定義。
   *
   * @param notification           汎用システム通知トピック
   * @param unstampedAlert         管理者向け未打刻アラートトピック
   * @param unstampedDirect        本人向け未打刻DMリマインドトピック
   * @param attendanceIrregularity 勤怠異常検知トピック
   */
  public record Topics(
      String notification,
      String unstampedAlert,
      String unstampedDirect,
      String attendanceIrregularity
  ) {

  }
}
