package com.computer_rescuer.attendance_management.adapter.out.hrmos.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * HRMOS 打刻ログ取得 API のレスポンス要素。
 * <p>
 * 整合性確保のため、日時は String で受け取り、マッパー層でパースを行います。
 * </p>
 *
 * @param userId          ユーザーID
 * @param createdAt       打刻時刻 (例: "2026-04-17T07:36:49.000+09:00")
 * @param stampType       打刻手段 (1:Web打刻等)
 * @param stampLodgmentId 打刻種別 (1:出勤, 2:退勤, 3:休憩開始, 4:休憩終了)
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record HrmosStampLog(
    Integer userId,
    String createdAt,
    Integer stampType,
    Integer stampLodgmentId
) {

  /**
   * このログが出勤打刻であるか判定します。
   */
  public boolean isClockIn() {
    // stampLodgmentId == 1 が「出勤」を意味する
    return Integer.valueOf(1).equals(stampLodgmentId);
  }
}
