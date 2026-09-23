package com.computer_rescuer.attendance_management.application.port.in;

import java.time.YearMonth;

/**
 * 月次勤怠サマリ集計および通知連携を統括する入力ユースケース。
 */
public interface MonthlyAttendanceSummaryUseCase {

  /**
   * 指定年月の月次勤怠サマリ集計を実行し、差分があれば Kafka へ連携します。
   *
   * @param targetMonth 集計対象年月
   */
  void execute(YearMonth targetMonth);
}
