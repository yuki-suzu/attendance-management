package com.computer_rescuer.attendance_management.adapter.in.scheduler;

import com.computer_rescuer.attendance_management.application.port.in.NotifyUnstampedAlertUseCase;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 未打刻アラートの定期実行（クーロンジョブ）を担当するスケジューラー。
 * <p>
 * このクラスは Input Adapter として機能し、決まった時刻にシステムを自動駆動させます。 実行時刻の設定は cron 式で定義されます。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UnstampedAlertScheduler {

  private final NotifyUnstampedAlertUseCase useCase;

  /**
   * スケジュール１
   */
  @Scheduled(cron = "${app.batch.alert-cron1}")
  // lockAtLeastFor: バッチが1秒で終わっても、1分間は他のサーバーに実行させない（短期間の重複発火を防止）
  @SchedulerLock(name = "UnstampedAlertScheduler_executeDailyAlert", lockAtLeastFor = "PT1M", lockAtMostFor = "PT5M")
  public void runMorningTask() {
    executeDailyAlert();
  }

  /**
   * スケジュール２
   */
  @Scheduled(cron = "${app.batch.alert-cron2}")
  // lockAtLeastFor: バッチが1秒で終わっても、1分間は他のサーバーに実行させない（短期間の重複発火を防止）
  @SchedulerLock(name = "UnstampedAlertScheduler_executeDailyAlert", lockAtLeastFor = "PT1M", lockAtMostFor = "PT5M")
  public void runNoonTask() {
    executeDailyAlert();
  }

  /**
   * バッチ処理実行
   */
  public void executeDailyAlert() {
    LocalDate today = LocalDate.now();
    log.info("【定期実行】{} の未打刻アラートバッチを自動起動します。", today);
    useCase.execute(today);
  }
}
