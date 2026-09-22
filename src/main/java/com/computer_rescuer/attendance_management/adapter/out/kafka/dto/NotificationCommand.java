package com.computer_rescuer.attendance_management.adapter.out.kafka.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * notification-service の汎用通知トピックへ送信するコマンドDTO。
 * <p>
 * システムエラー通知や管理者アラートなど、非同期で LINE WORKS メッセージをディスパッチする際に使用します。
 * </p>
 *
 * @param channelType     通知先チャネル種別（例: LINE_WORKS）
 * @param destinationType 宛先種別（CHANNEL: トークルーム, USER: 個別DM）
 * @param targetId        送信先の識別子（null の場合は notification-service 側のデフォルト宛先が適用される）
 * @param message         送信するメッセージ本文
 */
public record NotificationCommand(
    @JsonProperty("channel_type")
    String channelType,

    @JsonProperty("destination_type")
    String destinationType,

    @JsonProperty("target_id")
    String targetId,

    @JsonProperty("message")
    String message
) {

  /**
   * システム管理者向けエラー通知（LINE WORKS 個人DM）用のコマンドを生成します。
   *
   * @param message 送信する障害メッセージ本文
   * @return 構築された {@link NotificationCommand}
   */
  public static NotificationCommand ofSystemError(String message) {
    return new NotificationCommand("LINE_WORKS", "USER", null, message);
  }
}
