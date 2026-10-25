package com.computer_rescuer.attendance_management.adapter.in.web;

import com.computer_rescuer.attendance_management.adapter.in.model.ApiResponse;
import com.computer_rescuer.attendance_management.application.port.in.NotifyUnstampedAlertUseCase;
import io.swagger.v3.oas.annotations.Operation;
import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 勤怠アラート（未打刻通知など）を手動でトリガーするための REST コントローラー。
 * <p>
 * 定期実行バッチとは別に、運用時のテストや、エラー発生時の再実行（リトライ）の ための手動実行エンドポイントを提供します。
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/attendances/alerts")
@RequiredArgsConstructor
public class AttendanceAlertController {

  private final NotifyUnstampedAlertUseCase useCase;
  private final Clock clock;

  /**
   * 未打刻者の抽出および LINE WORKS への管理者通知を手動で実行します。
   *
   * @param date 実行対象の日付（省略時はシステム日付の「本日」が適用されます）
   * @return 処理結果（200 OK）
   */
  @Operation(
      summary = "未打刻アラートバッチ手動実行",
      description = "指定した対象日の未打刻者（出勤予定時刻を過ぎても打刻実績のない従業員）を抽出し、管理者向けサマリ通知および本人向けDMイベントを Kafka へ即時ディスパッチします。日付（date）を省略した場合は「本日」が適用されます。"
  )
  @PostMapping("/unstamped")
  public ResponseEntity<ApiResponse<String>> triggerUnstampedAlert(
      @RequestParam(required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
  ) {
    LocalDate targetDate = (date != null) ? date : LocalDate.now(clock);

    log.info("【手動実行】{} の未打刻アラート通知 API が呼び出されました。", targetDate);

    // ユースケース（Interactor）の呼び出し
    useCase.execute(targetDate);

    return ResponseEntity.ok(
        ApiResponse.success("アラート通知バッチの実行が完了しました。対象日: " + targetDate));
  }
}
