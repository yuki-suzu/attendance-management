package com.computer_rescuer.attendance_management.application.interactor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.computer_rescuer.attendance_management.adapter.out.exception.ExternalIntegrationException;
import com.computer_rescuer.attendance_management.application.port.out.FetchDailyWorkRecordPort;
import com.computer_rescuer.attendance_management.application.port.out.FetchSegmentPort;
import com.computer_rescuer.attendance_management.domain.model.DailyAttendance;
import com.computer_rescuer.attendance_management.domain.model.DailyWorkRecord;
import com.computer_rescuer.attendance_management.domain.model.Segment;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link GetDailyAttendanceInteractor} の単体テストクラス。
 * <p>
 * 出勤状況照会・集計ユースケースの責務である以下のオーケストレーション処理を網羅的に検証します:
 * <ul>
 *   <li>外部勤怠実績データおよび勤務区分マスタの取得と結合</li>
 *   <li>マスタの勤務区分ステータス（{@code status = 1}: 勤務）に基づく休日・休暇データの除外</li>
 *   <li>マスタに存在しない未知・イレギュラーな勤務区分の安全な通過（除外抑止）と出勤予定時刻の null フォールバック</li>
 *   <li>マスタ未存在かつ勤務区分名が非空白時の警告ログ出力条件分岐（C1 カバレッジ網羅）</li>
 *   <li>勤務区分タイトル重複時における先勝ちマージ（{@code putIfAbsent}）動作</li>
 *   <li>各外部ポート例外発生時における例外伝播（異常系）</li>
 * </ul>
 * ドメインモデル（{@link DailyAttendance}）の生成を伴いますが、外部 I/O はモック化して決定論的にテストを実行します。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetDailyAttendanceInteractor 単体テスト")
class GetDailyAttendanceInteractorTest {

  @Mock
  private FetchDailyWorkRecordPort fetchPort;

  @Mock
  private FetchSegmentPort segmentPort;

  @InjectMocks
  private GetDailyAttendanceInteractor interactor;

  private final LocalDate targetDate = LocalDate.of(2026, 4, 20);

  /**
   * テスト用の勤怠実績レコードを生成するヘルパーメソッド。
   *
   * @param userId          従業員ID
   * @param employeeNumber  社員番号
   * @param segmentTitle    勤務区分名
   * @param actualStartTime 実績出勤時刻
   * @param stampingTime    打刻時刻
   * @return 構築された {@link DailyWorkRecord}
   */
  private DailyWorkRecord createDailyWorkRecord(
      Integer userId,
      String employeeNumber,
      String segmentTitle,
      LocalTime actualStartTime,
      LocalTime stampingTime
  ) {
    return new DailyWorkRecord(
        userId,
        employeeNumber,
        "テスト 従業員",
        "システム開発部",
        targetDate,
        segmentTitle,
        1,
        actualStartTime,
        0,
        stampingTime
    );
  }

  /**
   * テスト用の勤務区分マスタを生成するヘルパーメソッド。
   *
   * @param id      勤務区分ID
   * @param title   勤務区分名
   * @param status  状態（1: 勤務, 2: 休日・休暇, 3: その他）
   * @param startAt 基準勤務開始時刻
   * @return 構築された {@link Segment}
   */
  private Segment createSegment(Integer id, String title, Integer status, LocalTime startAt) {
    return new Segment(
        id,
        title,
        title,
        status,
        startAt,
        LocalTime.of(18, 0, 0)
    );
  }

  @Nested
  @DisplayName("正常系シナリオ")
  class SuccessScenario {

    /**
     * 【テスト観点 / 責務】 正常系全工程の検証。勤務区分マスタ（status = 1: 勤務）に該当する勤怠実績が正常に結合され、 マスタの出勤予定時刻（startAt）が適用された
     * {@link DailyAttendance} が生成されることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>対象日: 2026-04-20</li>
     *   <li>勤務区分マスタ: "通常勤務"（status = 1, startAt = 09:00:00）</li>
     *   <li>勤怠実績: EMP001 が "通常勤務" で 08:50 に出勤打刻済み</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>出勤判定結果が 1 件返却されること</li>
     *   <li>予定時刻が 09:00:00、打刻時刻が 08:50:00、ステータスが ATTENDED であること</li>
     * </ul>
     */
    @Test
    @DisplayName("正常系: 勤務ステータスの勤務区分と実績が正しくマージされ出勤判定モデルが返却されること")
    void shouldFetchAndCombineAttendanceSuccessfully() {
      // [Given]
      Segment workingSegment = createSegment(1, "通常勤務", 1, LocalTime.of(9, 0, 0));
      when(segmentPort.fetchAll()).thenReturn(List.of(workingSegment));

      DailyWorkRecord record = createDailyWorkRecord(
          101, "EMP001", "通常勤務", LocalTime.of(8, 50, 0), LocalTime.of(8, 50, 0));
      when(fetchPort.fetchByDate(targetDate)).thenReturn(List.of(record));

      // [When]
      List<DailyAttendance> actual = interactor.execute(targetDate);

      // [Then]
      assertThat(actual).hasSize(1);
      DailyAttendance result = actual.getFirst();
      assertThat(result.userId()).isEqualTo(101);
      assertThat(result.employeeNumber()).isEqualTo("EMP001");
      assertThat(result.segmentTitle()).isEqualTo("通常勤務");
      assertThat(result.scheduledStartAt()).isEqualTo(LocalTime.of(9, 0, 0));
      assertThat(result.stampingAt()).isEqualTo(LocalTime.of(8, 50, 0));
      assertThat(result.status()).isEqualTo(DailyAttendance.Status.ATTENDED);

      verify(fetchPort).fetchByDate(targetDate);
      verify(segmentPort).fetchAll();
    }
  }

  @Nested
  @DisplayName("境界値および C1 分岐シナリオ")
  class BoundaryAndBranchScenario {

    /**
     * 【テスト観点 / 責務】 休日・休暇区分（status != 1）のフィルタリング除外検証（C1 カバレッジ）。 マスタの status が 2（休日・休暇）や
     * 3（その他）のレコードが集計対象外として安全に除外されることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>Record 1: "法定休日"（status = 2: 休日・休暇）</li>
     *   <li>Record 2: "特別研修"（status = 3: その他）</li>
     *   <li>Record 3: "通常勤務"（status = 1: 勤務）</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>Record 1 と Record 2 が除外され、Record 3 のみ 1 件が返却されること</li>
     * </ul>
     */
    @Test
    @DisplayName("C1分岐: マスタの状態が勤務（status=1）以外の休日・その他区分レコードを除外すること")
    void shouldFilterOutNonWorkingStatusSegments() {
      // [Given]
      Segment holidaySeg = createSegment(2, "法定休日", 2, null);
      Segment otherSeg = createSegment(3, "特別研修", 3, null);
      Segment workSeg = createSegment(1, "通常勤務", 1, LocalTime.of(9, 0, 0));
      when(segmentPort.fetchAll()).thenReturn(List.of(holidaySeg, otherSeg, workSeg));

      DailyWorkRecord recHoliday = createDailyWorkRecord(101, "EMP001", "法定休日", null, null);
      DailyWorkRecord recOther = createDailyWorkRecord(102, "EMP002", "特別研修", null, null);
      DailyWorkRecord recWork = createDailyWorkRecord(103, "EMP003", "通常勤務", null,
          LocalTime.of(9, 0));

      when(fetchPort.fetchByDate(targetDate)).thenReturn(List.of(recHoliday, recOther, recWork));

      // [When]
      List<DailyAttendance> actual = interactor.execute(targetDate);

      // [Then]
      assertThat(actual).hasSize(1);
      assertThat(actual.getFirst().employeeNumber()).isEqualTo("EMP003");
      assertThat(actual.getFirst().segmentTitle()).isEqualTo("通常勤務");
    }

    /**
     * 【テスト観点 / 責務】 マスタ未存在の勤務区分の安全通過および警告ログ判定（C1 カバレッジ）の検証。 以下の 3 パターンすべてについて、除外されずに出勤判定対象として通過し、
     * 予定時刻が null として扱われることを確認します:
     * <ul>
     *   <li>パターン A: segmentTitle がマスタにない文字列（例: "臨時応援"） -> 警告ログ条件通過</li>
     *   <li>パターン B: segmentTitle が null -> 警告ログ条件不成立</li>
     *   <li>パターン C: segmentTitle が空白文字（"   "） -> 警告ログ条件不成立</li>
     * </ul>
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>勤務区分マスタは "通常勤務" のみ定義されていること</li>
     *   <li>上記 パターン A, B, C の勤怠レコードを入力すること</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>3 件とも除外されずに返却されること</li>
     *   <li>3 件すべての {@code scheduledStartAt} が null であること</li>
     * </ul>
     */
    @Test
    @DisplayName("C1分岐: マスタ未存在勤務区分の安全通過（予定時刻null）と警告ログ条件の各分岐網羅")
    void shouldHandleUnregisteredSegmentsSafelyWithNullScheduledStart() {
      // [Given]
      Segment masterSeg = createSegment(1, "通常勤務", 1, LocalTime.of(9, 0, 0));
      when(segmentPort.fetchAll()).thenReturn(List.of(masterSeg));

      // パターン A: 非空白の未登録名称（ログ出力ルート）
      DailyWorkRecord recUnregistered = createDailyWorkRecord(201, "EMP201", "臨時応援", null,
          null);
      // パターン B: segmentTitle が null（!null ルート）
      DailyWorkRecord recNullTitle = createDailyWorkRecord(202, "EMP202", null, null, null);
      // パターン C: segmentTitle が空白（!isBlank ルート）
      DailyWorkRecord recBlankTitle = createDailyWorkRecord(203, "EMP203", "   ", null, null);

      when(fetchPort.fetchByDate(targetDate)).thenReturn(
          List.of(recUnregistered, recNullTitle, recBlankTitle)
      );

      // [When]
      List<DailyAttendance> actual = interactor.execute(targetDate);

      // [Then]
      assertThat(actual).hasSize(3);

      DailyAttendance att1 = actual.get(0);
      assertThat(att1.employeeNumber()).isEqualTo("EMP201");
      assertThat(att1.segmentTitle()).isEqualTo("臨時応援");
      assertThat(att1.scheduledStartAt()).isNull();

      DailyAttendance att2 = actual.get(1);
      assertThat(att2.employeeNumber()).isEqualTo("EMP202");
      assertThat(att2.segmentTitle()).isNull();
      assertThat(att2.scheduledStartAt()).isNull();

      DailyAttendance att3 = actual.get(2);
      assertThat(att3.employeeNumber()).isEqualTo("EMP203");
      assertThat(att3.segmentTitle()).isEqualTo("   ");
      assertThat(att3.scheduledStartAt()).isNull();
    }

    /**
     * 【テスト観点 / 責務】 勤務区分マスタで同一タイトルが重複している場合の先勝ちマージ（{@code putIfAbsent}）動作を検証します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>マスタ内に "シフトA"（id = 1, startAt = 08:00）と "シフトA"（id = 2, startAt = 10:00）が存在すること</li>
     *   <li>勤怠実績として "シフトA" のレコードが存在すること</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>先頭に登録されている id = 1 の出勤予定時刻（08:00）が採用されること</li>
     * </ul>
     */
    @Test
    @DisplayName("境界値: 勤務区分名が重複している場合、先勝ち（putIfAbsent）で最初の予定時刻が採用されること")
    void shouldAdoptFirstSegmentWhenTitleIsDuplicated() {
      // [Given]
      Segment firstSeg = createSegment(1, "シフトA", 1, LocalTime.of(8, 0, 0));
      Segment duplicateSeg = createSegment(2, "シフトA", 1, LocalTime.of(10, 0, 0));
      when(segmentPort.fetchAll()).thenReturn(List.of(firstSeg, duplicateSeg));

      DailyWorkRecord record = createDailyWorkRecord(101, "EMP001", "シフトA", null, null);
      when(fetchPort.fetchByDate(targetDate)).thenReturn(List.of(record));

      // [When]
      List<DailyAttendance> actual = interactor.execute(targetDate);

      // [Then]
      assertThat(actual).hasSize(1);
      // 先勝ちの 08:00:00 が採用されていること
      assertThat(actual.getFirst().scheduledStartAt()).isEqualTo(LocalTime.of(8, 0, 0));
    }

    /**
     * 【テスト観点 / 責務】 勤怠実績が 0 件の場合の境界値検証。 ループが 0 回で正常に空リストを返却することを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>{@code fetchPort.fetchByDate} が空リストを返却すること</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>空リストが返却されること</li>
     * </ul>
     */
    @Test
    @DisplayName("境界値: 勤怠実績データが0件の場合、空リストを返却すること")
    void shouldReturnEmptyListWhenRecordsIsEmpty() {
      // [Given]
      when(segmentPort.fetchAll()).thenReturn(
          List.of(createSegment(1, "通常勤務", 1, LocalTime.of(9, 0))));
      when(fetchPort.fetchByDate(targetDate)).thenReturn(Collections.emptyList());

      // [When]
      List<DailyAttendance> actual = interactor.execute(targetDate);

      // [Then]
      assertThat(actual).isEmpty();
    }

    /**
     * 【テスト観点 / 責務】 勤務区分マスタが 0 件の場合の境界値検証。 マスタが空でも例外を発生させず、全実績が安全に通過して処理されることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>{@code segmentPort.fetchAll} が空リストを返却すること</li>
     *   <li>勤怠実績が 1 件存在すること</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>勤怠実績が除外されずに 1 件返却され、予定時刻が null であること</li>
     * </ul>
     */
    @Test
    @DisplayName("境界値: 勤務区分マスタが0件の場合でも、全勤怠実績が安全に処理されること")
    void shouldHandleEmptySegmentsGracefully() {
      // [Given]
      when(segmentPort.fetchAll()).thenReturn(Collections.emptyList());

      DailyWorkRecord record = createDailyWorkRecord(101, "EMP001", "通常勤務", null, null);
      when(fetchPort.fetchByDate(targetDate)).thenReturn(List.of(record));

      // [When]
      List<DailyAttendance> actual = interactor.execute(targetDate);

      // [Then]
      assertThat(actual).hasSize(1);
      assertThat(actual.getFirst().scheduledStartAt()).isNull();
    }
  }

  @Nested
  @DisplayName("異常系シナリオ")
  class ExceptionScenario {

    /**
     * 【テスト観点 / 責務】 勤怠実績取得ポート（{@link FetchDailyWorkRecordPort}）で外部通信例外が発生した場合に、
     * 例外を隠蔽せずに上位へ伝播させることを検証します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>{@code fetchPort.fetchByDate} が {@link ExternalIntegrationException} をスローすること</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>{@code assertThatThrownBy} で同例外が捕捉されること</li>
     *   <li>勤務区分マスタ取得ポートは呼び出されないこと</li>
     * </ul>
     */
    @Test
    @DisplayName("異常系: 勤怠実績取得ポートで例外が発生した場合、例外がそのまま伝播すること")
    void shouldPropagateExceptionWhenDailyWorkRecordFetchFails() {
      // [Given]
      when(fetchPort.fetchByDate(targetDate))
          .thenThrow(new ExternalIntegrationException("HRMOS API 通信障害"));

      // [When & Then]
      assertThatThrownBy(() -> interactor.execute(targetDate))
          .isInstanceOf(ExternalIntegrationException.class)
          .hasMessage("HRMOS API 通信障害");

      verifyNoInteractions(segmentPort);
    }

    /**
     * 【テスト観点 / 責務】 勤務区分マスタ取得ポート（{@link FetchSegmentPort}）で DB アクセス例外が発生した場合に、 例外が上位へ伝播することを検証します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>{@code fetchPort.fetchByDate} は正常に空リストを返却すること</li>
     *   <li>{@code segmentPort.fetchAll} が {@link RuntimeException} をスローすること</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>{@code assertThatThrownBy} で同例外が捕捉されること</li>
     * </ul>
     */
    @Test
    @DisplayName("異常系: 勤務区分マスタ取得ポートで例外が発生した場合、例外がそのまま伝播すること")
    void shouldPropagateExceptionWhenSegmentFetchFails() {
      // [Given]
      when(fetchPort.fetchByDate(targetDate)).thenReturn(Collections.emptyList());
      when(segmentPort.fetchAll()).thenThrow(new RuntimeException("データベース接続タイムアウト"));

      // [When & Then]
      assertThatThrownBy(() -> interactor.execute(targetDate))
          .isInstanceOf(RuntimeException.class)
          .hasMessage("データベース接続タイムアウト");
    }
  }
}
