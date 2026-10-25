package com.computer_rescuer.attendance_management.adapter.in.scheduler;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;

import com.computer_rescuer.attendance_management.application.port.in.SyncEmployeeUseCase;
import com.computer_rescuer.attendance_management.application.port.in.SyncMasterDataUseCase;
import com.computer_rescuer.attendance_management.application.port.out.SendErrNoticePort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link SyncMasterScheduler} の単体テストクラス。
 * <p>
 * アプリケーション起動時のイベントドリブン実行、クーロンによる定期実行、および 起動時障害発生におけるフェイルセーフ（例外捕捉と管理者通知）の命令網羅（C0 100%）を検証します。 Spring
 * コンテキストを起動せず、純粋な Mockito 単体テストとして高速に実行します。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SyncMasterScheduler 単体テスト")
class SyncMasterSchedulerTest {

  @Mock
  private SyncMasterDataUseCase syncMasterDataUseCase;

  @Mock
  private SyncEmployeeUseCase syncEmployeeUseCase;

  @Mock
  private SendErrNoticePort sendErrNoticePort;

  @InjectMocks
  private SyncMasterScheduler scheduler;

  @Nested
  @DisplayName("アプリケーション起動時同期（runOnStartup）シナリオ")
  class RunOnStartupScenario {

    /**
     * アプリケーション起動時に、マスタデータ同期および従業員マスタ同期が 正常順序で呼び出されることを検証します。
     */
    @Test
    @DisplayName("正常系: 起動時にマスタデータおよび従業員マスタの同期処理が順次呼び出されること")
    void shouldExecuteSyncAllMasterSuccessfullyOnStartup() {
      // [Given]
      willDoNothing().given(syncMasterDataUseCase).syncMasterData();
      willDoNothing().given(syncEmployeeUseCase).syncEmployees();

      // [When]
      scheduler.runOnStartup();

      // [Then]
      then(syncMasterDataUseCase).should().syncMasterData();
      then(syncEmployeeUseCase).should().syncEmployees();
      then(sendErrNoticePort).shouldHaveNoInteractions();
    }

    /**
     * 起動時同期処理で例外が発生した場合でも、アプリケーションの起動をクラッシュさせずに例外を飲み込み、
     * 障害通知ポート（{@link SendErrNoticePort}）へエラーメッセージが送信されることを検証します。
     */
    @Test
    @DisplayName("異常系: 起動時同期に失敗した場合、例外をスローせず捕捉してエラー通知を送信すること")
    void shouldCatchExceptionAndSendNoticeWhenStartupSyncFails() {
      // [Given]
      String failureDetail = "HRMOS 認証トークンの取得に失敗しました";
      willThrow(new RuntimeException(failureDetail)).given(syncMasterDataUseCase).syncMasterData();

      // [When & Then]
      assertDoesNotThrow(
          () -> scheduler.runOnStartup(),
          "メインスレッドのクラッシュを防止するため、いかなる例外も外へスローされないこと"
      );

      ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
      then(sendErrNoticePort).should().send(captor.capture());

      String capturedMessage = captor.getValue();
      assertAll(
          () -> assertTrue(
              capturedMessage.contains("🚨 【起動時エラー】マスター情報の初期同期に失敗しました。"),
              "ヘッダーが含まれること"),
          () -> assertTrue(capturedMessage.contains(failureDetail),
              "根本原因のエラー内容が含まれること")
      );
      then(syncEmployeeUseCase).shouldHaveNoInteractions();
    }
  }

  @Nested
  @DisplayName("スケジュール定期実行（runScheduledTask）シナリオ")
  class RunScheduledTaskScenario {

    /**
     * 定期実行タスクにおいて、マスタデータ同期および従業員マスタ同期が正しく呼び出されることを検証します。
     */
    @Test
    @DisplayName("定期スケジュール実行時、マスタデータおよび従業員マスタの同期が実行されること")
    void shouldExecuteSyncAllMasterOnScheduledTask() {
      // [Given]
      willDoNothing().given(syncMasterDataUseCase).syncMasterData();
      willDoNothing().given(syncEmployeeUseCase).syncEmployees();

      // [When]
      scheduler.runScheduledTask();

      // [Then]
      then(syncMasterDataUseCase).should().syncMasterData();
      then(syncEmployeeUseCase).should().syncEmployees();
      then(sendErrNoticePort).shouldHaveNoInteractions();
    }
  }
}
