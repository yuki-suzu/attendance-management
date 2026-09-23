package com.computer_rescuer.attendance_management.adapter.in.scheduler;

import com.computer_rescuer.attendance_management.application.port.out.CheckedEmployeeRepositoryPort;
import com.computer_rescuer.attendance_management.application.port.out.MonthlyAttendanceSummaryRepositoryPort;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * データライフサイクルを管理するスケジューラー。
 * <p>
 * 保持期間を超過した不要な判定履歴レコードを定期的に物理削除し、 テーブルサイズの肥大化とインデックス性能の劣化を防止します。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TableDataCleanupScheduler {

  private final CheckedEmployeeRepositoryPort checkedEmployeeRepositoryPort;
  private final MonthlyAttendanceSummaryRepositoryPort summaryRepositoryPort;

  /**
   * 過去データを削除するライフサイクル処理
   * <p>
   * 業務時間外に分散ロック（ShedLock）を獲得して実行されます。
   * </p>
   */
  @Scheduled(cron = "${app.batch.cleanup-cron}")
  @SchedulerLock(
      name = "CheckedEmployeeCleanupScheduler_purgeExpiredRecords",
      lockAtLeastFor = "PT1M",
      lockAtMostFor = "PT10M"
  )
  public void purgeExpiredRecords() {
    LocalDateTime now = LocalDateTime.now();
    purgeExpiredCheckedEmployeeRecords(now);
    purgeExpiredMonthlyReportRecords(now);
  }

  /**
   * チェック済み従業員履歴削除処理
   *
   * @param procDatetime 処理基準日
   */
  private void purgeExpiredCheckedEmployeeRecords(LocalDateTime procDatetime) {
    LocalDate thresholdDate = LocalDate.from(procDatetime.minusYears(1));
    log.info("【データライフサイクル】{} より古いチェック済み従業員履歴のパージを開始します。",
        thresholdDate);

    try {
      int deletedCount = checkedEmployeeRepositoryPort.deleteOlderThan(thresholdDate);
      log.info("✅ 【データライフサイクル完了】{} 件の古い履歴レコードをパージしました。",
          deletedCount);
    } catch (Exception e) {
      log.error("❌ 【データライフサイクル異常】履歴レコードのパージ中にエラーが発生しました", e);
    }
  }

  /**
   * 月次勤怠サマリ削除処理
   *
   * @param procDatetime 処理基準日
   */
  private void purgeExpiredMonthlyReportRecords(LocalDateTime procDatetime) {
    LocalDateTime threshold = procDatetime.minusMonths(6);
    log.info("【データライフサイクル】{} より古い月次勤怠サマリのパージを開始します。", threshold);
    try {
      int count = summaryRepositoryPort.deleteOlderThan(threshold);
      log.info("✅ 【データライフサイクル完了】{} 件の古い月次サマリをパージしました。", count);
    } catch (Exception e) {
      log.error("❌ 【データライフサイクル異常】月次サマリのパージに失敗しました", e);
    }
  }
}
