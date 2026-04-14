package com.computer_rescuer.attendance_management.domain.model;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 外部システム（HRMOS等）から取得した、特定日の「純粋な勤怠実績データ」を表現するドメインモデル。
 * <p>
 * このレコードは、打刻時間や実績時間といった「事実（Fact）」のみを保持するための入れ物です。<br> 「遅刻」や「出勤済」といった業務ルールに基づく状態判定は含まれておらず、
 * アプリケーション層（Interactor）が判定を行うためのクリーンな入力データとして機能します。
 * </p>
 *
 * @param userId            従業員を一意に識別する内部ID
 * @param employeeNumber    従業員に付与された社員番号
 * @param fullName          従業員のフルネーム
 * @param date              勤怠実績の対象となる日付
 * @param segmentTitle      割り当てられている勤務区分の名称（例: "出勤:残業あり"）
 * @param applicationStatus 申請区分のステータス（1. 未申請, 2. 承認待ち, 3. 承認済み, 4. 再申請(差し戻し)）
 * @param actualStartTime   外部システム側で計算・丸め処理、または手動修正された後の「出勤実績時刻」。給与計算等の基準となる時刻です。
 * @param nextDayStart      出勤実績時刻が日またぎ（前日:-1, 当日:0, 翌日:1）であるかを示すフラグ
 * @param stampingTime      従業員がタイムレコーダー等で実際に打刻した「純粋な打刻時刻」。丸め処理や手動修正の影響を受けていない生のデータです。
 */
public record DailyWorkRecord(
    Integer userId,
    String employeeNumber,
    String fullName,
    LocalDate date,
    String segmentTitle,
    Integer applicationStatus,
    LocalTime actualStartTime,
    Integer nextDayStart,
    LocalTime stampingTime
) {

}