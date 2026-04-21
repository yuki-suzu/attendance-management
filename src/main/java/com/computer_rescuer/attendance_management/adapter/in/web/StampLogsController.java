package com.computer_rescuer.attendance_management.adapter.in.web;

import com.computer_rescuer.attendance_management.adapter.in.exception.InvalidRequestParameterException;
import com.computer_rescuer.attendance_management.application.port.in.GetStampLogsUseCase;
import com.computer_rescuer.attendance_management.domain.model.StampLog;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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

  /**
   * 全従業員の特定日の打刻履歴リストを取得します。
   *
   * @param date 対象日。未指定の場合はシステム日付（当日）が自動的に適用されます。
   * @return 打刻履歴のリスト
   */
  @GetMapping("/daily")
  public List<StampLog> getDaily(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    LocalDate targetDate = (date != null) ? date : LocalDate.now();
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
  @GetMapping("/users/{employeeNumber}")
  public List<StampLog> getByUser(
      @PathVariable String employeeNumber,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

// 日付の逆転チェックのみ実施（400 Bad Request を返すのが理想的です）
    if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
      throw new InvalidRequestParameterException(
          "開始日(fromDate)が終了日(toDate)より未来に設定されています。");
    }

    return useCase.getUserLogs(employeeNumber, fromDate, toDate);
  }
}
