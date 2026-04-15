package com.computer_rescuer.attendance_management.adapter.in.web;

import com.computer_rescuer.attendance_management.application.interactor.NotifyUnstampedAlertInteractor;
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

  private final NotifyUnstampedAlertInteractor notifyUnstampedAlertInteractor;

  /**
   * 未打刻者の抽出および LINE WORKS への管理者通知を手動で実行します。
   *
   * @param date 実行対象の日付（省略時はシステム日付の「本日」が適用されます）
   * @return 処理結果（200 OK）
   */
  @PostMapping("/unstamped")
  public ResponseEntity<String> triggerUnstampedAlert(
      @RequestParam(required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
  ) {
    LocalDate targetDate = (date != null) ? date : LocalDate.now();

    log.info("【手動実行】{} の未打刻アラート通知 API が呼び出されました。", targetDate);

    // ユースケース（Interactor）の呼び出し
    notifyUnstampedAlertInteractor.execute(targetDate);

    return ResponseEntity.ok("アラート通知バッチの実行が完了しました。対象日: " + targetDate);
  }
}
