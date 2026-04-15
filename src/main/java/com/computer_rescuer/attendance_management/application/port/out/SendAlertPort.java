package com.computer_rescuer.attendance_management.application.port.out;

/**
 * システムからの警告やアラートメッセージを外部（管理者など）へ送信するための出力ポート。
 * <p>
 * Application層は、このインターフェースを通じて通知を依頼します。<br> 実際の送信先が LINE WORKS なのか、Slack なのか、メールなのかといった
 * インフラの実装詳細は、このポートを実装する Adapter 側で決定されます。
 * </p>
 */
public interface SendAlertPort {

  /**
   * アラートメッセージを送信します。
   *
   * @param message 送信するアラートの本文
   */
  void send(String message);
}
