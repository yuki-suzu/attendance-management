package com.computer_rescuer.attendance_management.adapter.in.web;

import com.computer_rescuer.attendance_management.adapter.in.model.ApiResponse;
import com.computer_rescuer.attendance_management.application.port.in.GetDailyAttendanceUseCase;
import com.computer_rescuer.attendance_management.domain.model.DailyAttendance;
import io.swagger.v3.oas.annotations.Operation;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
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
  private final Clock clock;

  /**
   * 全従業員の出勤状況リストを取得します。
   *
   * @param date 対象日（yyyy-MM-dd）。未指定の場合は当日。
   * @return 出勤判定結果リスト
   */
  @Operation(
      summary = "全従業員日次出勤状況一覧取得",
      description = "HRMOS の勤怠実績および内部勤務区分マスタを突き合わせ、指定日における全従業員の出勤判定結果（出勤済、未出勤、遅刻/打刻忘れ等）の一覧を取得します。日付（date）を省略した場合は「本日」が適用されます。"
  )
  @GetMapping
  public ResponseEntity<ApiResponse<List<DailyAttendance>>> getAttendance(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    LocalDate targetDate = (date != null) ? date : LocalDate.now(clock);
    return ResponseEntity.ok(ApiResponse.success(useCase.execute(targetDate)));
  }
}
