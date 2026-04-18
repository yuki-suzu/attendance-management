package com.computer_rescuer.attendance_management.domain.model;

import java.time.LocalDateTime;

/**
 * 従業員の1回の「打刻事実」を表現するドメインモデル。
 * <p>
 * 外部API（HRMOS）のレスポンスである生データに対し、ローカルDB（m_employee）の
 * 従業員マスタ情報を結合（エンリッチ）し、アプリケーション内で扱いやすい完全なデータとして保持します。
 * </p>
 *
 * @param userId         HRMOSシステムにおけるユーザーID（ローカルDBの id と一致）
 * @param employeeNumber 自社システムにおける従業員の社員番号（結合により取得）
 * @param lastName       姓（結合により取得）
 * @param firstName      名（結合により取得）
 * @param stampingAt     実際に打刻された日時（日本時間）
 * @param stampType      打刻のアクション種別（Enum）
 * @param userAgent      UserAgent（打刻端末情報）
 */
public record StampLog(
    Integer userId,
    String employeeNumber,
    String lastName,
    String firstName,
    LocalDateTime stampingAt,
    StampType stampType,
    String userAgent
) {

  /**
   * この打刻が出勤打刻であるかを判定します。
   *
   * @return 出勤打刻の場合は true
   */
  public boolean isClockIn() {
    return this.stampType == StampType.CLOCK_IN;
  }

  /**
   * この打刻が退勤打刻であるかを判定します。
   *
   * @return 退勤打刻の場合は true
   */
  public boolean isClockOut() {
    return this.stampType == StampType.CLOCK_OUT;
  }
}
