package com.computer_rescuer.attendance_management.adapter.in.scheduler;

import com.computer_rescuer.attendance_management.application.port.in.SyncEmployeeUseCase;
import com.computer_rescuer.attendance_management.application.port.in.SyncMasterDataUseCase;
import com.computer_rescuer.attendance_management.application.port.out.SendErrNoticePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 各種マスタ同期の定期実行（クーロンジョブ）を担当するスケジューラー。
 * <p>
 * このクラスは Input Adapter として機能し、初回起動時＋決まった時刻にシステムを自動駆動させます。 実行時刻の設定は cron 式で定義されます。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SyncMasterScheduler {

  private final SyncMasterDataUseCase syncMasterDataUseCase;
  private final SyncEmployeeUseCase syncEmployeeUseCase;
  private final SendErrNoticePort sendErrNoticePort;

  /**
   * アプリ起動直後に一回実行する
   */
  @EventListener(ApplicationReadyEvent.class)
  public void runOnStartup() {
    log.info("【初期起動】 システム起動に伴うマスター情報同期処理を開始します。");
    try {
      executeSyncAllMaster();
    } catch (Exception e) {
      // 💡 メインスレッド（起動時）のエラーはハンドラーが拾えないので、ここで捕まえて通知する
      log.error("❌ 初期起動時のマスター同期に失敗しました", e);
      sendErrNoticePort.send(
          "🚨 【起動時エラー】マスター情報の初期同期に失敗しました。\n詳細: "
              + ExceptionUtils.getRootCauseMessage(e));
      // ※ ここで throw してしまうとアプリが起動しなくなる可能性があるため、通知だけして飲み込む
    }
  }

  /**
   * スケジュール２
   */
  @Scheduled(cron = "${app.batch.sync-master-cron}")
  // lockAtLeastFor: バッチが1秒で終わっても、1分間は他のサーバーに実行させない（短期間の重複発火を防止）
  @SchedulerLock(name = "SyncMasterScheduler_executeSyncAllMaster", lockAtLeastFor = "PT1M", lockAtMostFor = "PT5M")
  public void runScheduledTask() {
    log.info("【定期実行】 スケジュールによるマスター情報同期処理を開始します。");
    executeSyncAllMaster();
  }

  /**
   * バッチ処理実行
   */
  private void executeSyncAllMaster() {
    log.info("▶ マスター情報（拠点・雇用形態など）の同期を開始します...");
    syncMasterDataUseCase.syncMasterData();

    log.info("▶ 従業員情報の同期を開始します...");
    syncEmployeeUseCase.syncEmployees();

    log.info("✅ 同期バッチ処理が完了しました。");
  }
}
