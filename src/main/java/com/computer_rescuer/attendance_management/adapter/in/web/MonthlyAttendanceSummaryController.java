package com.computer_rescuer.attendance_management.adapter.in.web;

import com.computer_rescuer.attendance_management.adapter.in.model.ApiResponse;
import com.computer_rescuer.attendance_management.application.port.in.MonthlyAttendanceSummaryUseCase;
import java.time.YearMonth;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 月次勤怠サマリの手動実行 API を提供するコントローラー。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/attendances/summary/monthly")
@RequiredArgsConstructor
public class MonthlyAttendanceSummaryController {

  private final MonthlyAttendanceSummaryUseCase useCase;

  /**
   * 月次勤怠サマリの集計・差分通知を手動でキックします。
   *
   * @param month 対象月（yyyy-MM 形式。未指定時は当月）
   * @return 実行結果
   */
  @PostMapping
  public ResponseEntity<ApiResponse<String>> triggerMonthlySummary(
      @RequestParam(required = false) String month
  ) {
    YearMonth targetMonth = (month != null && !month.isBlank())
        ? YearMonth.parse(month)
        : YearMonth.now();

    log.info("【手動実行】{} の月次勤怠サマリ集計 API が呼び出されました。", targetMonth);
    useCase.execute(targetMonth);

    return ResponseEntity.ok(ApiResponse.success(
        String.format("%s の月次勤怠サマリ集計・通知処理が完了しました。", targetMonth)
    ));
  }
}
