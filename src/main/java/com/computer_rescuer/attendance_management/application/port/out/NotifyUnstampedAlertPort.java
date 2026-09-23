package com.computer_rescuer.attendance_management.application.port.out;

import com.computer_rescuer.attendance_management.adapter.out.kafka.dto.UnstampedAlertEvent;
import com.computer_rescuer.attendance_management.adapter.out.kafka.dto.UnstampedDirectReminderEvent;

/**
 * 未打刻アラートイベントを外部メッセージング基盤へ送信するための出力ポート。
 * <p>
 * 管理者向けサマリー通知および本人向け個別DMリマインドの非同期ディスパッチ責務を定義します。
 * </p>
 */
public interface NotifyUnstampedAlertPort {

  /**
   * 管理者向けの未打刻サマリーアラートイベントを送信します。
   *
   * @param event 管理者向け未打刻アラートイベントDTO
   */
  void sendManagerAlert(UnstampedAlertEvent event);

  /**
   * 従業員本人向けの未打刻個別DMリマインドイベントを送信します。
   *
   * @param event 本人向けリマインドイベントDTO
   */
  void sendDirectReminder(UnstampedDirectReminderEvent event);
}
