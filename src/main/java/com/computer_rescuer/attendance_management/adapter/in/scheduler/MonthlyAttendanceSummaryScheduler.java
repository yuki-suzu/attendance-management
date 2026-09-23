package com.computer_rescuer.attendance_management.adapter.in.scheduler;

import com.computer_rescuer.attendance_management.application.port.in.MonthlyAttendanceSummaryUseCase;
import java.time.YearMonth;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 月次勤怠サマリ集計の定期実行スケジューラー。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MonthlyAttendanceSummaryScheduler {

  private final MonthlyAttendanceSummaryUseCase useCase;

  /**
   * 当月分の月次サマリを自動集計・通知します。
   */
  @Scheduled(cron = "${app.batch.monthly-summary-cron}")
  @SchedulerLock(
      name = "MonthlyAttendanceSummaryScheduler_executeDailySummary",
      lockAtLeastFor = "PT1M",
      lockAtMostFor = "PT10M"
  )
  public void executeDailySummary() {
    YearMonth currentMonth = YearMonth.now();
    log.info("【定期実行】{} の月次勤怠サマリ自動集計を開始します。", currentMonth);
    useCase.execute(currentMonth);
  }
}
