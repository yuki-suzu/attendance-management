package com.computer_rescuer.attendance_management.application.port.out;

import com.computer_rescuer.attendance_management.adapter.out.hrmos.model.HrmosStampLog;
import java.time.LocalDate;
import java.util.List;

/**
 * HRMOSから生の打刻ログ（Stamps）を取得するための出力ポート。
 * <p>
 * 日次勤怠データの反映待ちを回避し、リアルタイムな打刻事実を確認するために使用します。
 * </p>
 */
public interface FetchHrmosStampPort {

  /**
   * 指定された日付の全ユーザーの打刻ログを取得します。
   *
   * @param date 対象日
   * @return 打刻ログのリスト
   */
  List<HrmosStampLog> fetchByDate(LocalDate date);
}
