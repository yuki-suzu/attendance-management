package com.computer_rescuer.attendance_management.adapter.in.web;

import com.computer_rescuer.attendance_management.adapter.in.exception.InvalidRequestParameterException;
import com.computer_rescuer.attendance_management.adapter.in.model.ApiResponse;
import com.computer_rescuer.attendance_management.application.port.in.GetStampLogsUseCase;
import com.computer_rescuer.attendance_management.domain.model.StampLog;
import io.swagger.v3.oas.annotations.Operation;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 従業員の打刻履歴に関するリクエストを処理するWebコントローラー。
 * <p>
 * フロントエンドからのリクエストを受け付け、UseCase（アプリケーション層）へ処理を委譲します。
 * </p>
 */
@RestController
@RequestMapping("/api/v1/stamp-logs")
@RequiredArgsConstructor
public class StampLogsController {

  private final GetStampLogsUseCase useCase;
  private final Clock clock;

  /**
   * 全従業員の特定日の打刻履歴リストを取得します。
   *
   * @param date 対象日。未指定の場合はシステム日付（当日）が自動的に適用されます。
   * @return 打刻履歴のリスト
   */
  @Operation(
      summary = "全従業員日次打刻履歴取得",
      description = "HRMOS API から指定日における全従業員の打刻ログ生データを取得し、ローカル DB の従業員マスタ情報でエンリッチした打刻事実リストを取得します。日付（date）を省略した場合は「本日」が適用されます。"
  )
  @GetMapping("/daily")
  public List<StampLog> getDaily(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    LocalDate targetDate = (date != null) ? date : LocalDate.now(clock);
    return useCase.getDailyLogs(targetDate);
  }

  /**
   * 指定ユーザーの特定期間における打刻履歴リストを取得します。
   *
   * @param employeeNumber 従業員番号（パス変数として受け取る）
   * @param fromDate       抽出開始日。未指定の場合は当日。
   * @param toDate         抽出終了日。未指定の場合は当日。
   * @return 打刻履歴のリスト
   */
  @Operation(
      summary = "指定従業員期間打刻履歴取得",
      description = "社員番号（employeeNumber）で指定された従業員の、特定期間における打刻履歴を取得します。開始日（fromDate）が終了日（toDate）より未来の場合は 400 Bad Request エラーとなります。"
  )
  @GetMapping("/users/{employeeNumber}")
  public ResponseEntity<ApiResponse<List<StampLog>>> getByUser(
      @PathVariable String employeeNumber,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

    // 日付の逆転チェックのみ実施
    if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
      throw new InvalidRequestParameterException(
          "開始日(fromDate)が終了日(toDate)より未来に設定されています。");
    }

    return ResponseEntity.ok(
        ApiResponse.success(useCase.getUserLogs(employeeNumber, fromDate, toDate)));
  }
}
