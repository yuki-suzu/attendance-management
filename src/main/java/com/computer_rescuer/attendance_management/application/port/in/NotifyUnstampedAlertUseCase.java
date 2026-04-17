package com.computer_rescuer.attendance_management.application.port.in;

import java.time.LocalDate;

/**
 * 未打刻アラート通知ユースケースのインターフェース。
 * <p>
 * 指定された日付において、始業予定時刻を過ぎても打刻が確認できない従業員を特定し、 適切な通知先へアラートを送信する責務を定義します。
 * </p>
 */
public interface NotifyUnstampedAlertUseCase {

  /**
   * 未打刻者の検知および通知処理を実行します。
   *
   * @param date 対象日（yyyy-MM-dd）
   */
  void execute(LocalDate date);
}
