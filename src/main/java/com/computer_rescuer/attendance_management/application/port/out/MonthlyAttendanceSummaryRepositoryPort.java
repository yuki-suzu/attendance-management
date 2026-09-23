package com.computer_rescuer.attendance_management.application.port.out;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 月次勤怠サマリテーブル（{@code t_monthly_attendance_summary}）の出力ポート。
 */
public interface MonthlyAttendanceSummaryRepositoryPort {

  /**
   * サマリ登録用データキャリア。
   */
  record SummaryItem(
      Integer segmentId,
      String segmentTitle,
      int count,
      String msgSend
  ) {

  }

  /**
   * 永続化されたサマリレコード。
   */
  record PersistedSummary(
      String procMonth,
      String employeeNumber,
      Integer segmentId,
      String segmentTitle,
      int count,
      String msgSend
  ) {

  }

  /**
   * 特定従業員の月次勤怠サマリを一括 UPSERT します。
   * <p>
   * 既存レコードと count が異なる場合のみ msg_send を '0' に倒します。
   * </p>
   */
  void upsertSummaries(String procMonth, String employeeNumber, List<SummaryItem> items);

  /**
   * 指定月において、未送信フラグ（msg_send = '0'）が 1 件でも存在する従業員番号の一覧を取得します。
   */
  List<String> findEmployeeNumbersWithUnsentSummary(String procMonth);

  /**
   * 指定された従業員番号群の、当月サマリレコード全件を取得します。
   */
  List<PersistedSummary> findSummariesByEmployeeNumbers(String procMonth,
      List<String> employeeNumbers);

  /**
   * 送信完了した従業員の当月レコードの msg_send をすべて '1' に更新します。
   */
  void markAsSent(String procMonth, List<String> employeeNumbers);

  /**
   * 最終更新日時（updated_at）が指定日時より古いレコードを物理削除します。
   */
  int deleteOlderThan(LocalDateTime thresholdDateTime);
}
