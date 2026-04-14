package com.computer_rescuer.attendance_management.application.port.in;

import com.computer_rescuer.attendance_management.domain.model.DailyAttendance;
import java.time.LocalDate;
import java.util.List;

/**
 * 指定日の出勤状況を判定・取得するユースケース。
 */
public interface GetDailyAttendanceUseCase {

  /**
   * 指定した日付の全従業員の出勤状況を判定して返します。
   *
   * @param targetDate 対象日
   * @return 出勤判定結果のリスト
   */
  List<DailyAttendance> execute(LocalDate targetDate);
}
