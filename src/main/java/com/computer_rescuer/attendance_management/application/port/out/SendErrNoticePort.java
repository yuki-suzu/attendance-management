package com.computer_rescuer.attendance_management.application.port.out;

/**
 * システム例外発生時等にメッセージを外部（管理者）へ送信するための出力ポート。
 * <p>
 * Application層は、このインターフェースを通じて通知を依頼します。<br> 実際の送信先が LINE WORKS なのか、Slack なのか、メールなのかといった
 * インフラの実装詳細は、このポートを実装する Adapter 側で決定されます。
 * </p>
 */
public interface SendErrNoticePort {

  /**
   * エラー通知メッセージを送信します。
   *
   * @param message 送信する通知の本文
   */
  void send(String message);
}
