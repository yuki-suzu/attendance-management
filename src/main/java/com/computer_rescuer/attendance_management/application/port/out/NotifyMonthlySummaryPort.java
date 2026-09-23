package com.computer_rescuer.attendance_management.application.port.out;

import com.computer_rescuer.attendance_management.adapter.out.kafka.dto.AttendanceIrregularityEvent;

/**
 * 月次勤怠サマリ（勤怠異常）の通知イベントをメッセージング基盤へ送信する出力ポート。
 */
public interface NotifyMonthlySummaryPort {

  /**
   * 月次勤怠サマリ通知イベントを Kafka トピックへ Publish します。
   *
   * @param event 月次勤怠サマリエベント
   */
  void sendMonthlySummary(AttendanceIrregularityEvent event);
}
