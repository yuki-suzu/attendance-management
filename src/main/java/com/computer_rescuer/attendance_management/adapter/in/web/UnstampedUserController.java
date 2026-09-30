package com.computer_rescuer.attendance_management.adapter.in.web;

import com.computer_rescuer.attendance_management.adapter.in.model.ApiResponse;
import com.computer_rescuer.attendance_management.application.port.in.ExtractUnstampedUsersUseCase;
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
 * 打刻漏れ（未打刻）の従業員に関する操作を提供するRESTコントローラー。
 */
@RestController
@RequestMapping("/api/v1/attendances/unstamped")
@RequiredArgsConstructor
public class UnstampedUserController {

  private final ExtractUnstampedUsersUseCase useCase;
  private final Clock clock;

  /**
   * 指定した日付における、出勤予定時刻を過ぎて未打刻の従業員一覧を取得します。
   *
   * @param date 抽出対象の日付（未指定時はシステム日付の本日）
   * @return 未打刻者のリスト
   */
  @Operation(
      summary = "未打刻（遅刻・打刻忘れ）従業員一覧取得",
      description = "指定した対象日において、出勤予定時刻を過ぎているにも関わらず出勤打刻が確認できない従業員（Status = LATE_OR_FORGOT）のみを抽出して一覧返却します。日付（date）を省略した場合は「本日」が適用されます。"
  )
  @GetMapping
  public ResponseEntity<ApiResponse<List<DailyAttendance>>> getUnstampedUsers(
      @RequestParam(required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
  ) {
    // 日付指定がなければ「今日」を対象とする
    LocalDate targetDate = (date != null) ? date : LocalDate.now(clock);

    return ResponseEntity.ok(ApiResponse.success(useCase.execute(targetDate)));
  }
}
