package com.computer_rescuer.attendance_management.adapter.in.web;

import com.computer_rescuer.attendance_management.adapter.in.model.ApiResponse;
import com.computer_rescuer.attendance_management.application.port.in.MonthlyAttendanceSummaryUseCase;
import io.swagger.v3.oas.annotations.Operation;
import java.time.Clock;
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
  private final Clock clock;

  /**
   * 月次勤怠サマリの集計・差分通知を手動でキックします。
   *
   * @param month 対象月（yyyy-MM 形式。未指定時は当月）
   * @return 実行結果
   */
  @Operation(
      summary = "月次勤怠サマリ集計・通知手動実行",
      description = "指定年月の全従業員の勤怠実績（予定休・当欠・半休・遅延）を集計して DB へ UPSERT し、前回送信時からの変動差分が検知された従業員情報を Kafka トピックへ通知発行します。対象年月（month）を省略した場合は「当月」が適用されます。"
  )
  @PostMapping
  public ResponseEntity<ApiResponse<String>> triggerMonthlySummary(
      @RequestParam(required = false) String month
  ) {
    YearMonth targetMonth = (month != null && !month.isBlank())
        ? YearMonth.parse(month)
        : YearMonth.now(clock);

    log.info("【手動実行】{} の月次勤怠サマリ集計 API が呼び出されました。", targetMonth);
    useCase.execute(targetMonth);

    return ResponseEntity.ok(ApiResponse.success(
        String.format("%s の月次勤怠サマリ集計・通知処理が完了しました。", targetMonth)
    ));
  }
}
