package com.computer_rescuer.attendance_management.adapter.out.hrmos.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * HRMOS 日次勤怠取得 API のレスポンスモデル。
 * <p>
 * パースエラーによるシステム停止を防ぐため、日時はすべて String で安全に受け取ります。
 * </p>
 *
 * @param userId              ユーザーID
 * @param number              社員番号
 * @param fullName            氏名
 * @param month               年月
 * @param day                 日付 (例: "2026-04-14")
 * @param wday                曜日
 * @param segmentDisplayTitle 勤務区分表示名
 * @param segmentTitle        勤務区分名
 * @param status              申請区分のステータス
 * @param startAt             出勤時刻の実績値 (例: "2000-01-01T09:00:00.000+09:00" または "09:00")
 * @param nextDayStart        出勤時刻の実績値が前日/当日/翌日かを示すフラグ
 * @param stampingStartAt     純粋な打刻時刻 (例: "2000-01-01T09:00:00.000+09:00" または "09:00" または null)
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record HrmosDailyWorkOutput(
    Integer userId,
    String number,
    String fullName,
    String month,
    String day,
    String wday,
    String segmentDisplayTitle,
    String segmentTitle,
    Integer status,
    String startAt,
    Integer nextDayStart,
    String stampingStartAt
) {

}
