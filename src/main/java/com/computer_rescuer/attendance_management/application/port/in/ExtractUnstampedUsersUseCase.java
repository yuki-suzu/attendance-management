package com.computer_rescuer.attendance_management.application.port.in;

import com.computer_rescuer.attendance_management.domain.model.DailyAttendance;
import java.time.LocalDate;
import java.util.List;

/**
 * 予定時刻を過ぎているにも関わらず、未打刻の従業員を抽出するユースケース。
 * <p>
 * このユースケースは、リアルタイムな打刻漏れチェックや、 Slack/チャットツール等へのリマインド通知バッチの起点として利用されます。
 * </p>
 */
public interface ExtractUnstampedUsersUseCase {

  /**
   * 指定された日付において、出勤予定時刻を過ぎているのに未打刻の従業員一覧を取得します。
   *
   * @param targetDate 抽出対象となる日付（通常は本日）
   * @return 未打刻（遅刻または打刻忘れ）と判定された従業員の勤怠情報リスト
   */
  List<DailyAttendance> execute(LocalDate targetDate);
}
