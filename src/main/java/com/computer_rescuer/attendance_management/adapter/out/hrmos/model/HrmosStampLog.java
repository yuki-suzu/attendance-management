package com.computer_rescuer.attendance_management.adapter.out.hrmos.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * HRMOS 打刻ログ取得 API のレスポンス要素（生データ）。
 * <p>
 * 外部APIのJSON仕様と1:1で対応するデータ転送オブジェクト（DTO）です。
 * </p>
 *
 * @param userId          ユーザーID
 * @param createdAt       打刻時刻 (例: "2026-04-17T07:36:49.000+09:00")
 * @param stampType       打刻のアクション種別（1:出勤, 2:退勤...）
 * @param stampLodgmentId 打刻種別詳細（拠点カスタム用・今回は判定に使用しない）
 * @param lodgmentId      旧仕様の打刻種別詳細
 * @param userAgent       打刻端末のUserAgent（デバッグ・調査用）
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record HrmosStampLog(
    Integer userId,
    String createdAt,
    Integer stampType,
    Integer stampLodgmentId,
    Integer lodgmentId,
    String userAgent
) {

  /**
   * このログが出勤系の打刻であるか判定します。
   */
  public boolean isClockIn() {
    return Integer.valueOf(1).equals(stampType);
  }
}
