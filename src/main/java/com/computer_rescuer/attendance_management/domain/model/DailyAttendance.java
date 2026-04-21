package com.computer_rescuer.attendance_management.domain.model;

import static com.computer_rescuer.attendance_management.shared.DateTimeConstants.JST;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;

/**
 * 特定の日付における従業員の「最終的な出勤判定結果」を保持するドメインモデル。
 * <p>
 * このクラスは単なるデータの入れ物ではなく、自身の状態（ステータス）を決定するための 業務ルール（ビジネスロジック）を内包したリッチドメインモデルです。<br>
 * 外部から与えられた「実績（Fact）」と「予定（Plan）」を元に、会社の就業規則に照らし合わせて 適切な {@link Status} を決定し、自身のインスタンスを生成します。
 * </p>
 *
 * @param userId           従業員を一意に識別する内部ID
 * @param employeeNumber   従業員に付与された社員番号
 * @param fullName         従業員のフルネーム
 * @param departmentName   所属名（多層階層結合済み） // 👈 追加
 * @param segmentTitle     割り当てられている勤務区分の名称
 * @param status           業務ロジックによって導き出された出勤ステータス
 * @param scheduledStartAt 本来出社すべき「出勤予定時刻」
 * @param actualStartAt    計算・修正後の「出勤実績時刻」
 * @param stampingAt       実際に記録した「純粋な打刻時刻」
 */
public record DailyAttendance(
    Integer userId,
    String employeeNumber,
    String fullName,
    String departmentName,
    String segmentTitle,
    Status status,
    LocalTime scheduledStartAt,
    LocalTime actualStartAt,
    LocalTime stampingAt
) {

  /**
   * 勤怠実績と予定時刻を元に、出勤判定ロジックを適用して {@code DailyAttendance} を生成します。
   */
  public static DailyAttendance create(
      DailyWorkRecord record,
      LocalTime scheduledStart,
      LocalDate targetDate,
      ZonedDateTime now
  ) {
    Status status = determineStatus(record, scheduledStart, targetDate, now);

    return new DailyAttendance(
        record.userId(),
        record.employeeNumber(),
        record.fullName(),
        record.departmentName(), // 👈 追加
        record.segmentTitle(),
        status,
        scheduledStart,
        record.actualStartTime(),
        record.stampingTime()
    );
  }

  /**
   * 勤怠実績と予定時刻、現在時刻を比較し、未打刻の人を出力する
   *
   * @param r              勤怠実績データ
   * @param scheduledStart 出勤予定時刻
   * @param targetDate     対象日
   * @param now            現在時刻
   * @return 判定された出勤ステータス
   */
  private static Status determineStatus(DailyWorkRecord r, LocalTime scheduledStart,
      LocalDate targetDate, ZonedDateTime now) {

    // 💡 ロジックを極限までシンプルに。
    // 「生の打刻時刻」があるか？ あれば、何時であれ「出勤済（打刻済）」
    if (r.stampingTime() != null) {
      return Status.ATTENDED;
    }

    // 打刻がなく、予定時刻が存在する場合
    if (scheduledStart != null) {
      LocalDateTime deadlineTime = LocalDateTime.of(targetDate, scheduledStart);

      // 夜勤等の補正
      if (r.nextDayStart() != null) {
        deadlineTime = deadlineTime.plusDays(r.nextDayStart());
      }

      // 💡 予定時刻を過ぎているのに、stampingTime が null なら「未打刻」確定
      if (now.isAfter(deadlineTime.atZone(JST))) {
        return Status.LATE_OR_FORGOT;
      }
    }

    return Status.NOT_ATTENDED;
  }

  /**
   * 従業員の当日の出勤状況を表す列挙型。
   */
  public enum Status {
    /**
     * 打刻実績、または手動修正された出勤実績が存在する状態
     */
    ATTENDED("出勤済"),
    /**
     * 打刻も実績もなく、かつ出勤予定時刻を過ぎていない状態
     */
    NOT_ATTENDED("未出勤"),
    /**
     * 打刻も実績もなく、出勤予定時刻を過ぎている状態
     */
    LATE_OR_FORGOT("遅刻 / 打刻忘れ");

    private final String label;

    Status(String label) {
      this.label = label;
    }

    /**
     * 画面表示等に使用するステータスの日本語ラベルを取得します。
     *
     * @return ステータスの日本語ラベル（例: "出勤済"）
     */
    public String getLabel() {
      return label;
    }
  }
}
