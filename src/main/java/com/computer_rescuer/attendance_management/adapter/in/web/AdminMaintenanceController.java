package com.computer_rescuer.attendance_management.adapter.in.web;

import com.computer_rescuer.attendance_management.adapter.in.model.ApiResponse;
import com.computer_rescuer.attendance_management.application.port.out.CheckedEmployeeRepositoryPort;
import com.computer_rescuer.attendance_management.application.port.out.MonthlyAttendanceSummaryRepositoryPort;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * システム管理者向けの運用保守・データメンテナンス API を提供するコントローラー。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/maintenance")
@RequiredArgsConstructor
public class AdminMaintenanceController {

  private final CheckedEmployeeRepositoryPort checkedEmployeeRepositoryPort;
  private final MonthlyAttendanceSummaryRepositoryPort summaryRepositoryPort;
  
  /**
   * チェック済み従業員履歴（t_checked_employee）のパージ処理を手動で実行します。
   *
   * @param beforeDate パージ対象の基準日（省略時は「本日より1年前」の日付が適用されます）
   * @return パージ完了メッセージおよび削除件数
   */
  @PostMapping("/cleanup/checked-employees")
  public ResponseEntity<ApiResponse<String>> purgeExpiredCheckedEmployees(
      @RequestParam(required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beforeDate
  ) {
    LocalDate thresholdDate = (beforeDate != null) ? beforeDate : LocalDate.now().minusYears(1);

    log.info("【手動実行】{} 以前のチェック済み従業員履歴パージ API が呼び出されました。",
        thresholdDate);

    int deletedCount = checkedEmployeeRepositoryPort.deleteOlderThan(thresholdDate);

    log.info("【手動実行完了】{} 件の履歴レコードを物理削除しました。", deletedCount);

    return ResponseEntity.ok(ApiResponse.success(
        String.format("基準日: %s 以前のチェック済み履歴 %d 件をパージしました。", thresholdDate,
            deletedCount)
    ));
  }

  @PostMapping("/cleanup/monthly-summaries")
  public ResponseEntity<ApiResponse<String>> purgeExpiredMonthlySummaries(
      @RequestParam(required = false)
      @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate beforeDate
  ) {
    LocalDateTime threshold = (beforeDate != null)
        ? beforeDate.atStartOfDay()
        : LocalDateTime.now().minusMonths(6);

    int count = summaryRepositoryPort.deleteOlderThan(threshold);
    return ResponseEntity.ok(ApiResponse.success(
        String.format("基準日: %s 以前の月次サマリ %d 件をパージしました。", threshold, count)
    ));
  }
}
