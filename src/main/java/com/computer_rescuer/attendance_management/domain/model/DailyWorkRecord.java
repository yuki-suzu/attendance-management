package com.computer_rescuer.attendance_management.domain.model;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 外部システム（HRMOS等）から取得した、特定日の「純粋な勤怠実績データ」を保持するドメインモデル。
 * <p>
 * このレコードは不変（イミュータブル）であり、状態を変更する場合は新しいインスタンスを生成して返す Wither パターンを採用しています。<br> ※
 * 出勤日かどうかの判定（isWorkingDay）は、このクラスではなく {@link Segment} クラスの責務へと移管されました。
 * </p>
 *
 * @param userId            従業員を一意に識別する内部ID
 * @param employeeNumber    従業員に付与された社員番号
 * @param fullName          従業員のフルネーム
 * @param departmentName    多層階層を結合した所属名（ローカルDBの情報を正とする）
 * @param date              勤怠実績の対象日
 * @param segmentTitle      割り当てられている勤務区分の名称（例: "通常勤務", "有休"）
 * @param applicationStatus 申請区分のステータス
 * @param actualStartTime   確定済みの出勤実績時刻
 * @param nextDayStart      日またぎフラグ
 * @param stampingTime      生の打刻時刻
 */
public record DailyWorkRecord(
    Integer userId,
    String employeeNumber,
    String fullName,
    String departmentName,
    LocalDate date,
    String segmentTitle,
    Integer applicationStatus,
    LocalTime actualStartTime,
    Integer nextDayStart,
    LocalTime stampingTime
) {

  /**
   * 外部から取得した「事実としての打刻時刻」を自身のデータに統合します。
   *
   * @param actualStamp 生の打刻ログから判明した最新の出勤時刻
   * @return 打刻時刻が更新された新しい DailyWorkRecord インスタンス
   */
  public DailyWorkRecord withStampingTime(LocalTime actualStamp) {
    if (actualStamp == null) {
      return this;
    }
    return new DailyWorkRecord(
        userId(), employeeNumber(), fullName(), departmentName(), date(),
        segmentTitle(), applicationStatus(), actualStartTime(), nextDayStart(),
        actualStamp
    );
  }

  /**
   * データベースから取得した正確な所属名で自身のデータを更新します。
   * <p>
   * HRMOS APIから返却される不確かな文字列ではなく、 システムのローカルマスタ（m_department）の値を正として適用するために使用します。
   * </p>
   *
   * @param newDepartmentName ローカルデータベースから取得した所属名
   * @return 所属名が更新された新しい DailyWorkRecord インスタンス
   */
  public DailyWorkRecord withDepartmentName(String newDepartmentName) {
    if (newDepartmentName == null) {
      return this;
    }
    return new DailyWorkRecord(
        userId(), employeeNumber(), fullName(), newDepartmentName, date(),
        segmentTitle(), applicationStatus(), actualStartTime(), nextDayStart(),
        stampingTime()
    );
  }
}