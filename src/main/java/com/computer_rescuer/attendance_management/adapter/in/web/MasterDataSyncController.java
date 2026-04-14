package com.computer_rescuer.attendance_management.adapter.in.web;

import com.computer_rescuer.attendance_management.application.port.in.SyncMasterDataUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 各種マスタデータ同期のAPIリクエストを受け付けるWebコントローラー。
 * <p>
 * HTTP通信の入り口として機能し、技術的なWeb関心事（ルーティング、ステータスコード）を処理した後、 実際のビジネスロジックは {@link SyncMasterDataUseCase}
 * に委譲します。
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/sync/master")
@RequiredArgsConstructor
public class MasterDataSyncController {

  private final SyncMasterDataUseCase syncMasterDataUseCase;

  /**
   * 外部システムから最新のマスタデータを取得し、自システムのマスタを同期（洗い替え）するAPI。
   * <p>
   * HTTP POST メソッドで呼び出され、同期処理をキックします。 現在は部門マスタおよび勤務区分マスタが対象です。
   * </p>
   *
   * @return 処理結果（成功時は HTTP 200 OK）
   */
  @PostMapping
  public ResponseEntity<Void> syncMasterData() {
    log.info("API要求受信: 各種マスタデータの同期処理をキックします。");

    syncMasterDataUseCase.syncMasterData();

    return ResponseEntity.ok().build();
  }
}
