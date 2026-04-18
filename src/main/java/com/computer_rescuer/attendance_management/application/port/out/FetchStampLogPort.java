package com.computer_rescuer.attendance_management.application.port.out;

import com.computer_rescuer.attendance_management.domain.model.StampLog;
import java.time.LocalDate;
import java.util.List;

/**
 * 外部システム（HRMOS等）から打刻履歴データを取得するための出力ポート。
 * <p>
 * アプリケーション層は、外部システムが具体的にどのようなAPI（REST, gRPC等）を 持っているかを知らずに、このインターフェースを通じてデータを要求します。
 * </p>
 */
public interface FetchStampLogPort {

  /**
   * 指定された日付の、全従業員の打刻ログを取得します。
   *
   * @param date 対象日
   * @return 打刻ログのリスト（ドメインモデル）
   */
  List<StampLog> fetchDailyLogs(LocalDate date);

  /**
   * 指定された従業員の、特定の期間内の打刻ログを取得します。
   *
   * @param employeeNumber 従業員番号
   * @param fromDate       開始日
   * @param toDate         終了日
   * @return 打刻ログのリスト（ドメインモデル）
   */
  List<StampLog> fetchUserLogs(String employeeNumber, LocalDate fromDate, LocalDate toDate);
}
