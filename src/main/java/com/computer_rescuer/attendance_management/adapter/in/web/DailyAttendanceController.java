package com.computer_rescuer.attendance_management.adapter.in.web;

import com.computer_rescuer.attendance_management.application.port.in.GetDailyAttendanceUseCase;
import com.computer_rescuer.attendance_management.domain.model.DailyAttendance;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 従業員の出勤状況に関するリクエストを処理するコントローラー。
 */
@RestController
@RequestMapping("/api/v1/attendance")
@RequiredArgsConstructor
public class DailyAttendanceController {

  private final GetDailyAttendanceUseCase useCase;

  /**
   * 全従業員の出勤状況リストを取得します。
   *
   * @param date 対象日（yyyy-MM-dd）。未指定の場合は当日。
   * @return 出勤判定結果リスト
   */
  @GetMapping
  public List<DailyAttendance> getAttendance(@RequestParam(required = false) String date) {
    LocalDate targetDate = (date != null) ? LocalDate.parse(date) : LocalDate.now();
    return useCase.execute(targetDate);
  }
}
