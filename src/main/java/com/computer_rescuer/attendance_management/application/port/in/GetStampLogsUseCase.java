package com.computer_rescuer.attendance_management.application.port.in;

import com.computer_rescuer.attendance_management.domain.model.StampLog;
import java.time.LocalDate;
import java.util.List;

/**
 * 打刻履歴を取得するためのユースケース（入力ポート）。
 * <p>
 * Webコントローラー等のクライアントに対して、打刻履歴の検索・取得機能を提供します。
 * </p>
 */
public interface GetStampLogsUseCase {

  /**
   * 指定した日付における、全従業員の打刻履歴を取得します。
   *
   * @param date 対象日
   * @return 対象日の打刻履歴リスト
   */
  List<StampLog> getDailyLogs(LocalDate date);

  /**
   * 指定した従業員の、特定期間における打刻履歴を取得します。
   *
   * @param employeeNumber 従業員番号
   * @param fromDate       抽出開始日
   * @param toDate         抽出終了日
   * @return 該当期間の打刻履歴リスト
   */
  List<StampLog> getUserLogs(String employeeNumber, LocalDate fromDate, LocalDate toDate);
}
