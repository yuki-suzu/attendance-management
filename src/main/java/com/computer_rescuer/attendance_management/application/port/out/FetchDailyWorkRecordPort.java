package com.computer_rescuer.attendance_management.application.port.out;

import com.computer_rescuer.attendance_management.domain.model.DailyWorkRecord;
import java.time.LocalDate;
import java.util.List;

/**
 * 指定した日付の勤怠実績データを外部システムから取得する出力ポート。
 */
public interface FetchDailyWorkRecordPort {

  /**
   * 指定日の全従業員の勤怠実績データを取得します。
   *
   * @param date 対象日
   * @return 勤怠実績データのリスト
   */
  List<DailyWorkRecord> fetchByDate(LocalDate date);
}
