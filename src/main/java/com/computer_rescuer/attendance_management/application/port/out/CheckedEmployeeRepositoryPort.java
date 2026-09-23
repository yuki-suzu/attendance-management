package com.computer_rescuer.attendance_management.application.port.out;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * チェック済み従業員情報（{@code t_checked_employee}）のデータアクセスを抽象化する出力ポート。
 * <p>
 * アラート通知済み判定（デデュープ）や当月累積回数の算出、および検知結果の一括記録など、 未打刻判定プロセスに必要な永続化操作を提供します。
 * </p>
 */
public interface CheckedEmployeeRepositoryPort {

  /**
   * 未打刻検知（遅刻・打刻忘れ）を表す完了事由コード。
   */
  String REASON_CODE_UNSTAMPED = "01";

  /**
   * 指定日において、すでにチェック（通知処理）が完了している従業員IDのセットを取得します。
   * <p>
   * 本人向け個別DM通知の当日内多重送信を防止するための判定材料として使用します。
   * </p>
   *
   * @param targetDate  対象日
   * @param employeeIds 対象従業員のシステム内部IDリスト
   * @return チェック済みとして記録されている従業員IDのセット（存在しない場合は空セット）
   */
  Set<Integer> findCheckedEmployeeIds(LocalDate targetDate, List<Integer> employeeIds);

  /**
   * 指定された対象日の月度（当月1日〜対象日）における、従業員ごとのチェック済み累積回数を集計します。
   *
   * @param targetDate  対象日（月度末尾の基準日）
   * @param employeeIds 集計対象従業員のシステム内部IDリスト
   * @return 従業員IDをキー、当月の累積チェック回数を値とするマップ
   */
  Map<Integer, Integer> countMonthlyChecked(LocalDate targetDate, List<Integer> employeeIds);

  /**
   * 未打刻検知された従業員情報をチェック済みテーブルへ一括記録します。
   * <p>
   * 既に同一日付・同一従業員IDでレコードが存在する場合は、例外を発生させず安全にスキップ（無視）します。
   * </p>
   *
   * @param targetDate  対象日
   * @param employeeIds 記録対象の従業員IDリスト
   * @param reasonCode  完了事由コード（例: {@link #REASON_CODE_UNSTAMPED}）
   */
  void saveAll(LocalDate targetDate, List<Integer> employeeIds, String reasonCode);

  /**
   * 指定された基準日より古いチェック済み履歴レコードを物理削除します。
   *
   * @param thresholdDate 削除対象の境界日（この日より前のレコードが削除対象）
   * @return 削除された件数
   */
  int deleteOlderThan(LocalDate thresholdDate);
}
