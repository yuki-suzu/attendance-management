package com.computer_rescuer.attendance_management.application.interactor;

import com.computer_rescuer.attendance_management.application.port.in.SyncMasterDataUseCase;
import com.computer_rescuer.attendance_management.application.port.out.FetchMasterDataPort;
import com.computer_rescuer.attendance_management.application.port.out.MasterDataRepositoryPort;
import com.computer_rescuer.attendance_management.domain.model.Department;
import com.computer_rescuer.attendance_management.domain.model.Segment;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 各種マスタデータ同期ユースケースの実装クラス（インタラクター）。
 * <p>
 * 入力ポート（{@link SyncMasterDataUseCase}）を実装し、 出力ポート（{@link FetchMasterDataPort},
 * {@link MasterDataRepositoryPort}）を組み合わせて データの取得から永続化までの一連のビジネスロジックをトランザクション制御下で実行します。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SyncMasterDataInteractor implements SyncMasterDataUseCase {

  private final FetchMasterDataPort fetchPort;
  private final MasterDataRepositoryPort repositoryPort;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void syncMasterData() {
    log.info("=== 各種マスタデータ（部門・勤務区分）の同期処理を開始します ===");

    // 1. 部門マスタの同期
    List<Department> departments = fetchPort.fetchAllDepartments();
    if (!departments.isEmpty()) {
      repositoryPort.syncDepartments(departments);
    } else {
      log.warn("取得した部門情報が0件のため、部門マスタの更新をスキップします。");
    }

    // 2. 勤務区分マスタの同期
    List<Segment> segments = fetchPort.fetchAllSegments();
    if (!segments.isEmpty()) {
      repositoryPort.syncSegments(segments);
    } else {
      log.warn("取得した勤務区分情報が0件のため、勤務区分マスタの更新をスキップします。");
    }

    // もしここでExceptionが発生すれば、部門も勤務区分も両方ロールバック（元の状態に復元）されます。

    log.info("=== 各種マスタデータの同期処理が正常に完了しました ===");
  }
}
