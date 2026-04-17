package com.computer_rescuer.attendance_management.application.port.in;

/**
 * 各種マスタデータ（部門、勤務区分など）の同期処理を要求する入力ポート（ユースケース）。
 * <p>
 * コントローラー等の外部アダプターから呼び出され、アプリケーション固有のビジネスルール （トランザクション境界、処理順序など）の実行をトリガーします。
 * </p>
 */
public interface SyncMasterDataUseCase {

  /**
   * 外部システムから最新のマスタデータを取得し、自システムのマスタを全件洗い替えます。
   * <p>
   * 複数のマスタ更新は単一のトランザクションとして扱われ、 いずれかの更新に失敗した場合は全体がロールバックされます。
   * </p>
   */
  void syncMasterData();
}
