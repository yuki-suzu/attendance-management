package com.computer_rescuer.attendance_management.domain.model;

import static com.computer_rescuer.attendance_management.shared.DateTimeSupports.JST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.computer_rescuer.attendance_management.domain.model.DailyAttendance.Status;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link DailyAttendance} の単体テストクラス。
 * <p>
 * 勤怠ドメインモデルの核となる以下のビジネスロジックおよび状態判定を網羅的に検証します:
 * <ul>
 *   <li>打刻事実（{@code stampingTime}）が存在する場合の「出勤済（{@link Status#ATTENDED}）」確定判定</li>
 *   <li>打刻がなく予定時刻前、および予定時刻と同刻（境界値）における「未出勤（{@link Status#NOT_ATTENDED}）」判定</li>
 *   <li>打刻がなく予定時刻を超過した場合の「遅刻 / 打刻忘れ（{@link Status#LATE_OR_FORGOT}）」判定</li>
 *   <li>夜勤・翌日始業フラグ（{@code nextDayStart}）による締め切り日時の跨ぎ補正ロジック</li>
 *   <li>予定時刻が未設定（null）の場合の安全フォールバック</li>
 *   <li>ファクトリメソッド（{@link DailyAttendance#create}）による各属性値の完全な転送・保持</li>
 *   <li>{@link Status} 列挙型の表示ラベル定義</li>
 * </ul>
 * 分岐網羅率（C1 カバレッジ 100%）および命令網羅率（C0 カバレッジ 100%）を達成する設計としています。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DailyAttendance ドメインモデル単体テスト")
class DailyAttendanceTest {

  @Mock
  private DailyWorkRecord mockRecord;

  private final LocalDate targetDate = LocalDate.of(2026, 4, 20);
  private final LocalTime scheduledStart = LocalTime.of(9, 0, 0);

  /**
   * テスト用の {@link DailyWorkRecord} を組み立てるヘルパーメソッド。
   *
   * @param stampingTime 出勤打刻時刻
   * @param nextDayStart 日またぎフラグ（前日/当日/翌日補正日数）
   * @return 構築された {@link DailyWorkRecord}
   */
  private DailyWorkRecord buildRecord(LocalTime stampingTime, Integer nextDayStart) {
    return new DailyWorkRecord(
        101,
        "EMP001",
        "山田 太郎",
        "システム開発部",
        targetDate,
        "通常勤務",
        1,
        LocalTime.of(8, 55, 0),
        nextDayStart,
        stampingTime
    );
  }

  @Nested
  @DisplayName("出勤済（ATTENDED）判定シナリオ")
  class AttendedStatusScenario {

    /**
     * 【テスト観点 / 責務】 打刻時刻（stampingTime）が存在する場合、予定時刻より前の正常出勤において 確実に {@link Status#ATTENDED}
     * が返却されることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>予定出勤時刻: 09:00:00</li>
     *   <li>打刻時刻: 08:50:00（予定より前）</li>
     *   <li>現在日時: 2026-04-20 10:00:00 (JST)</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>判定ステータスが {@link Status#ATTENDED} であること</li>
     *   <li>保持されている stampingAt が 08:50:00 であること</li>
     * </ul>
     */
    @Test
    @DisplayName("正常打刻: 予定時刻前に打刻がある場合、出勤済（ATTENDED）と判定されること")
    void shouldDetermineAttendedWhenStampingTimeExistsBeforeScheduled() {
      // [Given]
      LocalTime stampTime = LocalTime.of(8, 50, 0);
      DailyWorkRecord record = buildRecord(stampTime, 0);
      ZonedDateTime now = ZonedDateTime.of(targetDate, LocalTime.of(10, 0, 0), JST);

      // [When]
      DailyAttendance actual = DailyAttendance.create(record, scheduledStart, targetDate, now);

      // [Then]
      assertThat(actual.status()).isEqualTo(Status.ATTENDED);
      assertThat(actual.stampingAt()).isEqualTo(stampTime);
    }

    /**
     * 【テスト観点 / 責務】 打刻時刻（stampingTime）が存在する場合、出勤予定時刻を過ぎた遅刻打刻であっても 「打刻事実」を最優先して
     * {@link Status#ATTENDED} と判定されることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>予定出勤時刻: 09:00:00</li>
     *   <li>打刻時刻: 09:30:00（遅刻出勤）</li>
     *   <li>現在日時: 2026-04-20 10:00:00 (JST)</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>遅刻であっても打刻済みのため {@link Status#ATTENDED} であること</li>
     * </ul>
     */
    @Test
    @DisplayName("遅刻打刻: 予定時刻超過後の打刻であっても、出勤済（ATTENDED）と判定されること")
    void shouldDetermineAttendedEvenWhenStampingTimeIsAfterScheduled() {
      // [Given]
      LocalTime lateStampTime = LocalTime.of(9, 30, 0);
      DailyWorkRecord record = buildRecord(lateStampTime, 0);
      ZonedDateTime now = ZonedDateTime.of(targetDate, LocalTime.of(10, 0, 0), JST);

      // [When]
      DailyAttendance actual = DailyAttendance.create(record, scheduledStart, targetDate, now);

      // [Then]
      assertThat(actual.status()).isEqualTo(Status.ATTENDED);
      assertThat(actual.stampingAt()).isEqualTo(lateStampTime);
    }

    /**
     * 【テスト観点 / 責務】 出勤予定時刻が未設定（null）であっても、打刻時刻が存在する場合は 正常に {@link Status#ATTENDED}
     * と判定されることを確認します（休日出勤等）。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>予定出勤時刻: null</li>
     *   <li>打刻時刻: 09:00:00</li>
     *   <li>現在日時: 2026-04-20 10:00:00 (JST)</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>ステータスが {@link Status#ATTENDED} であること</li>
     *   <li>予定時刻が null で保持されること</li>
     * </ul>
     */
    @Test
    @DisplayName("予定なし打刻: 予定時刻がnullでも打刻があれば出勤済（ATTENDED）と判定されること")
    void shouldDetermineAttendedWhenScheduledStartIsNullButStampingTimeExists() {
      // [Given]
      LocalTime stampTime = LocalTime.of(9, 0, 0);
      DailyWorkRecord record = buildRecord(stampTime, null);
      ZonedDateTime now = ZonedDateTime.of(targetDate, LocalTime.of(10, 0, 0), JST);

      // [When]
      DailyAttendance actual = DailyAttendance.create(record, null, targetDate, now);

      // [Then]
      assertThat(actual.status()).isEqualTo(Status.ATTENDED);
      assertThat(actual.scheduledStartAt()).isNull();
      assertThat(actual.stampingAt()).isEqualTo(stampTime);
    }
  }

  @Nested
  @DisplayName("未出勤（NOT_ATTENDED）判定シナリオ")
  class NotAttendedStatusScenario {

    /**
     * 【テスト観点 / 責務】 打刻時刻がなく、かつ現在時刻が出勤予定時刻より前の場合、
     * 「未出勤（{@link Status#NOT_ATTENDED}）」と正しく判定されることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>予定出勤時刻: 09:00:00</li>
     *   <li>打刻時刻: null</li>
     *   <li>現在日時: 2026-04-20 08:45:00 (JST)（始業前）</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>判定ステータスが {@link Status#NOT_ATTENDED} であること</li>
     * </ul>
     */
    @Test
    @DisplayName("始業前: 打刻がなく現在時刻が予定時刻より前の場合は未出勤（NOT_ATTENDED）と判定されること")
    void shouldDetermineNotAttendedWhenNowIsBeforeScheduledTime() {
      // [Given]
      DailyWorkRecord record = buildRecord(null, 0);
      ZonedDateTime now = ZonedDateTime.of(targetDate, LocalTime.of(8, 45, 0), JST);

      // [When]
      DailyAttendance actual = DailyAttendance.create(record, scheduledStart, targetDate, now);

      // [Then]
      assertThat(actual.status()).isEqualTo(Status.NOT_ATTENDED);
      assertThat(actual.stampingAt()).isNull();
    }

    /**
     * 【テスト観点 / 責務】 C1 分岐境界値（now.isAfter の false 判定）の検証。
     * 現在時刻が出勤予定時刻と「完全一致（09:00:00）」している瞬間は、超過（isAfter）ではないため 未打刻エラーとせず {@link Status#NOT_ATTENDED}
     * を維持することを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>予定出勤時刻: 09:00:00</li>
     *   <li>打刻時刻: null</li>
     *   <li>現在日時: 2026-04-20 09:00:00.000 (JST)（定刻ぴったり）</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>isAfter(deadline) が false となり、{@link Status#NOT_ATTENDED} であること</li>
     * </ul>
     */
    @Test
    @DisplayName("境界値（同刻）: 現在時刻が予定時刻と完全一致する場合、未出勤（NOT_ATTENDED）と判定されること")
    void shouldDetermineNotAttendedWhenNowIsExactlyAtScheduledTime() {
      // [Given]
      DailyWorkRecord record = buildRecord(null, 0);
      ZonedDateTime nowExact = ZonedDateTime.of(targetDate, scheduledStart, JST);

      // [When]
      DailyAttendance actual = DailyAttendance.create(record, scheduledStart, targetDate, nowExact);

      // [Then]
      assertThat(actual.status()).isEqualTo(Status.NOT_ATTENDED);
    }

    /**
     * 【テスト観点 / 責務】 C1 分岐（scheduledStart != null の false 判定）の検証。 打刻がなく、予定時刻も設定されていない（null）場合は、
     * 締め切り時刻の比較を行わずに安全に {@link Status#NOT_ATTENDED} と判定されることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>予定出勤時刻: null</li>
     *   <li>打刻時刻: null</li>
     *   <li>現在日時: 2026-04-20 15:00:00 (JST)</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>判定ステータスが {@link Status#NOT_ATTENDED} であること</li>
     * </ul>
     */
    @Test
    @DisplayName("C1分岐: 打刻がなく予定時刻がnullの場合は未出勤（NOT_ATTENDED）と判定されること")
    void shouldDetermineNotAttendedWhenScheduledStartIsNull() {
      // [Given]
      DailyWorkRecord record = buildRecord(null, null);
      ZonedDateTime now = ZonedDateTime.of(targetDate, LocalTime.of(15, 0, 0), JST);

      // [When]
      DailyAttendance actual = DailyAttendance.create(record, null, targetDate, now);

      // [Then]
      assertThat(actual.status()).isEqualTo(Status.NOT_ATTENDED);
    }
  }

  @Nested
  @DisplayName("遅刻 / 打刻忘れ（LATE_OR_FORGOT）判定シナリオ")
  class LateOrForgotStatusScenario {

    /**
     * 【テスト観点 / 責務】 打刻時刻がなく、現在時刻が出勤予定時刻を1秒でも超過している場合、 即座に「遅刻 /
     * 打刻忘れ（{@link Status#LATE_OR_FORGOT}）」と判定されることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>予定出勤時刻: 09:00:00</li>
     *   <li>打刻時刻: null</li>
     *   <li>現在日時: 2026-04-20 09:00:01 (JST)（1秒超過）</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>判定ステータスが {@link Status#LATE_OR_FORGOT} であること</li>
     * </ul>
     */
    @Test
    @DisplayName("予定超過: 打刻がなく予定時刻を1秒でも超過した場合、遅刻/打刻忘れ（LATE_OR_FORGOT）と判定されること")
    void shouldDetermineLateOrForgotWhenNowIsJustAfterScheduledTime() {
      // [Given]
      DailyWorkRecord record = buildRecord(null, 0);
      ZonedDateTime nowJustAfter = ZonedDateTime.of(
          targetDate, scheduledStart.plusSeconds(1), JST);

      // [When]
      DailyAttendance actual = DailyAttendance.create(record, scheduledStart, targetDate,
          nowJustAfter);

      // [Then]
      assertThat(actual.status()).isEqualTo(Status.LATE_OR_FORGOT);
    }

    /**
     * 【テスト観点 / 責務】 午前中の予定時刻に対し、昼休みの時間帯になっても打刻がない場合の未打刻検知を確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>予定出勤時刻: 09:00:00</li>
     *   <li>打刻時刻: null</li>
     *   <li>現在日時: 2026-04-20 12:00:00 (JST)</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>判定ステータスが {@link Status#LATE_OR_FORGOT} であること</li>
     * </ul>
     */
    @Test
    @DisplayName("大幅超過: 予定時刻を大幅に過ぎても打刻がない場合、遅刻/打刻忘れ（LATE_OR_FORGOT）と判定されること")
    void shouldDetermineLateOrForgotWhenNowIsWellPastScheduledTime() {
      // [Given]
      DailyWorkRecord record = buildRecord(null, 0);
      ZonedDateTime nowNoon = ZonedDateTime.of(targetDate, LocalTime.of(12, 0, 0), JST);

      // [When]
      DailyAttendance actual = DailyAttendance.create(record, scheduledStart, targetDate, nowNoon);

      // [Then]
      assertThat(actual.status()).isEqualTo(Status.LATE_OR_FORGOT);
    }
  }

  @Nested
  @DisplayName("夜勤・日またぎ補正（nextDayStart）シナリオ")
  class NextDayStartScenario {

    /**
     * 【テスト観点 / 責務】 C1 分岐（nextDayStart != null の true 判定）の検証。 夜勤等で
     * {@code nextDayStart = 1}（翌日始業）が設定されている場合、
     * 締め切り日時が翌日に繰り越され、当日の定刻を過ぎていても未出勤（NOT_ATTENDED）が維持されることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>対象日: 2026-04-20</li>
     *   <li>予定出勤時刻: 01:00:00</li>
     *   <li>nextDayStart: 1（実質 2026-04-21 01:00:00 が締め切り）</li>
     *   <li>現在日時: 2026-04-20 23:00:00 (JST)（当日中はまだ未始業）</li>
     *   <li>打刻時刻: null</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>翌日の 01:00 を迎えていないため、{@link Status#NOT_ATTENDED} であること</li>
     * </ul>
     */
    @Test
    @DisplayName("夜勤補正（始業前）: nextDayStart=1 の場合、締め切りが翌日に補正され当日中は未出勤（NOT_ATTENDED）となること")
    void shouldCompensateDeadlineForNextDayNightShiftBeforeStart() {
      // [Given]
      LocalTime nightShiftStart = LocalTime.of(1, 0, 0);
      DailyWorkRecord record = buildRecord(null, 1);
      // 当日の 23:00（締め切り 翌日 01:00 より前）
      ZonedDateTime now = ZonedDateTime.of(targetDate, LocalTime.of(23, 0, 0), JST);

      // [When]
      DailyAttendance actual = DailyAttendance.create(record, nightShiftStart, targetDate, now);

      // [Then]
      assertThat(actual.status()).isEqualTo(Status.NOT_ATTENDED);
    }

    /**
     * 【テスト観点 / 責務】 {@code nextDayStart = 1} で補正された翌日の締め切り時刻を超過した場合に、 正しく「遅刻 /
     * 打刻忘れ（{@link Status#LATE_OR_FORGOT}）」と判定されることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>対象日: 2026-04-20</li>
     *   <li>予定出勤時刻: 01:00:00</li>
     *   <li>nextDayStart: 1（締め切り: 2026-04-21 01:00:00）</li>
     *   <li>現在日時: 2026-04-21 01:05:00 (JST)（翌日始業時刻を超過）</li>
     *   <li>打刻時刻: null</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>補正後の締め切り時刻を超過したため、{@link Status#LATE_OR_FORGOT} であること</li>
     * </ul>
     */
    @Test
    @DisplayName("夜勤補正（超過後）: nextDayStart=1 の補正後締め切り時刻を超過した場合、遅刻/打刻忘れ（LATE_OR_FORGOT）となること")
    void shouldDetermineLateOrForgotAfterCompensatedNextDayDeadline() {
      // [Given]
      LocalTime nightShiftStart = LocalTime.of(1, 0, 0);
      DailyWorkRecord record = buildRecord(null, 1);
      // 翌日の 01:05（締め切り 翌日 01:00 を超過）
      ZonedDateTime nowNextDay = ZonedDateTime.of(
          targetDate.plusDays(1), LocalTime.of(1, 5, 0), JST);

      // [When]
      DailyAttendance actual = DailyAttendance.create(record, nightShiftStart, targetDate,
          nowNextDay);

      // [Then]
      assertThat(actual.status()).isEqualTo(Status.LATE_OR_FORGOT);
    }

    /**
     * 【テスト観点 / 責務】 C1 分岐（nextDayStart == null の false 判定）の検証。 nextDayStart が null
     * の場合、日付加算が行われず当日日付のまま締め切り時刻が判定されることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>nextDayStart: null</li>
     *   <li>予定時刻: 09:00:00</li>
     *   <li>現在日時: 2026-04-20 09:05:00 (JST)</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>当日基準で予定を超過しているため {@link Status#LATE_OR_FORGOT} であること</li>
     * </ul>
     */
    @Test
    @DisplayName("C1分岐: nextDayStart が null の場合、日付補正を行わず当日基準で超過判定すること")
    void shouldNotCompensateDaysWhenNextDayStartIsNull() {
      // [Given]
      DailyWorkRecord record = buildRecord(null, null);
      ZonedDateTime now = ZonedDateTime.of(targetDate, LocalTime.of(9, 5, 0), JST);

      // [When]
      DailyAttendance actual = DailyAttendance.create(record, scheduledStart, targetDate, now);

      // [Then]
      assertThat(actual.status()).isEqualTo(Status.LATE_OR_FORGOT);
    }
  }

  @Nested
  @DisplayName("ファクトリ生成属性転送および Status Enum の検証")
  class ModelAttributesAndEnumScenario {

    /**
     * 【テスト観点 / 責務】 {@link DailyAttendance#create} メソッドが、入力された {@link DailyWorkRecord} の全属性を
     * 欠落や取り違えなく自身のフィールドへマッピングしていることをモックを用いて厳格に検証します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>モックの DailyWorkRecord に各フィールド値を設定</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>全 getter / record コンポーネントが設定した値と完全に一致すること</li>
     * </ul>
     */
    @Test
    @DisplayName("属性整合性: DailyWorkRecord の全フィールド値が正確に DailyAttendance へコピーされること")
    void shouldMapAllRecordFieldsCorrectly() {
      // [Given]
      when(mockRecord.userId()).thenReturn(777);
      when(mockRecord.employeeNumber()).thenReturn("EMP777");
      when(mockRecord.fullName()).thenReturn("徳川 家康");
      when(mockRecord.departmentName()).thenReturn("幕府開発本部");
      when(mockRecord.segmentTitle()).thenReturn("裁量労働");
      when(mockRecord.actualStartTime()).thenReturn(LocalTime.of(9, 30, 0));
      when(mockRecord.stampingTime()).thenReturn(LocalTime.of(9, 15, 0));

      ZonedDateTime now = ZonedDateTime.of(targetDate, LocalTime.of(12, 0, 0), JST);

      // [When]
      DailyAttendance actual = DailyAttendance.create(
          mockRecord, scheduledStart, targetDate, now);

      // [Then]
      assertThat(actual.userId()).isEqualTo(777);
      assertThat(actual.employeeNumber()).isEqualTo("EMP777");
      assertThat(actual.fullName()).isEqualTo("徳川 家康");
      assertThat(actual.departmentName()).isEqualTo("幕府開発本部");
      assertThat(actual.segmentTitle()).isEqualTo("裁量労働");
      assertThat(actual.scheduledStartAt()).isEqualTo(scheduledStart);
      assertThat(actual.actualStartAt()).isEqualTo(LocalTime.of(9, 30, 0));
      assertThat(actual.stampingAt()).isEqualTo(LocalTime.of(9, 15, 0));
      assertThat(actual.status()).isEqualTo(Status.ATTENDED);
    }

    /**
     * 【テスト観点 / 責務】 {@link Status} 列挙型の全インスタンスについて、{@link Status#getLabel()} が
     * 定義された正しい日本語ラベルを返却することを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>EnumSource により Status の全値を網羅</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>各ステータスに対応するラベルが null または空文字でないこと</li>
     * </ul>
     */
    @ParameterizedTest
    @EnumSource(Status.class)
    @DisplayName("Enum定義: Status の各値に対応する日本語ラベルが正しく定義されていること")
    void shouldReturnCorrectLabelForEachStatus(Status status) {
      String expectedLabel = switch (status) {
        case ATTENDED -> "出勤済";
        case NOT_ATTENDED -> "未出勤";
        case LATE_OR_FORGOT -> "遅刻 / 打刻忘れ";
      };

      assertThat(status.getLabel()).isEqualTo(expectedLabel);
    }
  }
}
