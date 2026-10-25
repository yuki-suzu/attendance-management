package com.computer_rescuer.attendance_management.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.computer_rescuer.attendance_management.adapter.out.hrmos.model.HrmosDailyWorkOutput;
import com.computer_rescuer.attendance_management.application.port.out.MonthlyAttendanceSummaryRepositoryPort.SummaryItem;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link MonthlyAttendanceAggregator} の単体テストクラス。
 * <p>
 * HRMOS の日次勤怠データから月次勤怠異常（予定休・当欠・半休・遅延）を集計する 中核ドメインロジックの以下の仕様および分岐を網羅的に検証します:
 * <ul>
 *   <li>入力リストの null または空コレクションに対する安全なガード（空リスト返却）</li>
 *   <li>処理当日（上限日）より未来の実績レコードの除外フィルタリング</li>
 *   <li>勤務区分タイトル（{@code segmentTitle}）が null のレコードの除外</li>
 *   <li>不定期区分（当日休・午前半休・午後半休・遅延）が0件（予定休のみ等）の場合の登録不要判定（早期リターン）</li>
 *   <li>タイトル文字列の部分一致（{@code contains}）による日数の正確なカウント集計</li>
 *   <li>予定休は {@code msg_send = "1"}、その他の不定期区分は {@code msg_send = "0"} とするフラグ付与ルール</li>
 *   <li>実績件数が0件の勤務区分を結果サマリ項目（{@link SummaryItem}）に含めない除外ロジック</li>
 * </ul>
 * 外部依存のない純粋なドメインサービスであるため、モックを使用せず直接インスタンスを生成して高速にテストを実行します。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MonthlyAttendanceAggregator 単体テスト")
class MonthlyAttendanceAggregatorTest {

  @InjectMocks
  private MonthlyAttendanceAggregator aggregator;

  private final LocalDate today = LocalDate.of(2026, 4, 15);

  @BeforeEach
  void setUp() {
    aggregator = new MonthlyAttendanceAggregator();
  }

  /**
   * テスト用の {@link HrmosDailyWorkOutput} インスタンスを生成するヘルパーメソッド。
   *
   * @param day          日付文字列（yyyy-MM-dd）
   * @param segmentTitle 勤務区分名
   * @return 構築された {@link HrmosDailyWorkOutput}
   */
  private HrmosDailyWorkOutput createDailyOutput(String day, String segmentTitle) {
    return new HrmosDailyWorkOutput(
        101,
        "EMP001",
        "山田 太郎",
        "2026-04",
        day,
        "水",
        segmentTitle,
        segmentTitle,
        1,
        "09:00",
        0,
        "09:00",
        List.of("開発本部", "システム部")
    );
  }

  @Nested
  @DisplayName("入力ガードシナリオ（null・空コレクション）")
  class InputGuardScenario {

    /**
     * 【テスト観点 / 責務】 入力勤怠リストが null の場合の null 安全性を検証します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>{@code dailyOutputs} に null を渡すこと</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>例外がスローされず、空リストが返却されること</li>
     * </ul>
     */
    @Test
    @DisplayName("null 安全性: dailyOutputs が null の場合、空リストを返却すること")
    void shouldReturnEmptyListWhenDailyOutputsIsNull() {
      // [When]
      List<SummaryItem> actual = aggregator.aggregate(null, today);

      // [Then]
      assertThat(actual).isEmpty();
    }

    /**
     * 【テスト観点 / 責務】 入力勤怠リストが空コレクションの場合の早期リターンを検証します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>{@code dailyOutputs} に空リスト（{@code List.of()}）を渡すこと</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>空リストが返却されること</li>
     * </ul>
     */
    @Test
    @DisplayName("境界値: dailyOutputs が空リストの場合、空リストを返却すること")
    void shouldReturnEmptyListWhenDailyOutputsIsEmpty() {
      // [When]
      List<SummaryItem> actual = aggregator.aggregate(Collections.emptyList(), today);

      // [Then]
      assertThat(actual).isEmpty();
    }
  }

  @Nested
  @DisplayName("日付・項目フィルタリングシナリオ")
  class FilteringScenario {

    /**
     * 【テスト観点 / 責務】 処理日より未来日の勤怠実績が集計から確実に除外されること（境界値: 過去日、当日、翌日）を検証します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>基準日（today）: 2026-04-15</li>
     *   <li>過去日（2026-04-14）: 遅延 1件（集計対象）</li>
     *   <li>当日（2026-04-15）: 当日休 1件（集計対象）</li>
     *   <li>翌日（2026-04-16）: 当日休 1件（未来日のため集計から除外）</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>当日休が1件、遅延が1件として集計され、翌日の当日休は集計に含まれないこと</li>
     * </ul>
     */
    @Test
    @DisplayName("境界値: 基準日当日および過去日の実績のみを集計対象とし、未来日（翌日以降）の実績を除外すること")
    void shouldExcludeRecordsAfterToday() {
      // [Given]
      List<HrmosDailyWorkOutput> outputs = List.of(
          createDailyOutput("2026-04-14", "遅延"),
          createDailyOutput("2026-04-15", "当日休"),
          createDailyOutput("2026-04-16", "当日休") // 未来日のため除外されるべき
      );

      // [When]
      List<SummaryItem> actual = aggregator.aggregate(outputs, today);

      // [Then]
      Map<Integer, SummaryItem> resultMap = actual.stream()
          .collect(Collectors.toMap(SummaryItem::segmentId, item -> item));

      assertThat(resultMap)
          .containsKeys(
              MonthlyAttendanceAggregator.SEG_DELAY,
              MonthlyAttendanceAggregator.SEG_UNSCHEDULED_HOLIDAY
          );

      // 当日休は 4/15 の 1件のみ（4/16 分が加算されて 2件になっていないこと）
      assertThat(resultMap.get(MonthlyAttendanceAggregator.SEG_UNSCHEDULED_HOLIDAY).count())
          .isEqualTo(1);
      assertThat(resultMap.get(MonthlyAttendanceAggregator.SEG_DELAY).count())
          .isEqualTo(1);
    }

    /**
     * 【テスト観点 / 責務】 勤務区分タイトル（{@code segmentTitle}）が null のレコードが存在する場合でも、 NullPointerException
     * を発生させずに安全にスキップされることを検証します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>{@code segmentTitle} が null のレコードが1件</li>
     *   <li>有効な不定期区分（"遅延"）のレコードが1件</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>null のレコードが無視され、"遅延" 1件のみが正常に集計されること</li>
     * </ul>
     */
    @Test
    @DisplayName("C1分岐: segmentTitle が null のレコードを安全にスキップすること")
    void shouldFilterOutRecordsWithNullSegmentTitle() {
      // [Given]
      List<HrmosDailyWorkOutput> outputs = List.of(
          createDailyOutput("2026-04-10", null),
          createDailyOutput("2026-04-11", "遅延")
      );

      // [When]
      List<SummaryItem> actual = aggregator.aggregate(outputs, today);

      // [Then]
      assertThat(actual).hasSize(1);
      SummaryItem item = actual.getFirst();
      assertThat(item.segmentId()).isEqualTo(MonthlyAttendanceAggregator.SEG_DELAY);
      assertThat(item.count()).isEqualTo(1);
    }
  }

  @Nested
  @DisplayName("不定期区分判定および登録要否判定シナリオ")
  class IrregularCountScenario {

    /**
     * 【テスト観点 / 責務】 不定期区分（当日休、午前・午後半休、遅延）が0件で「予定休」や「通常勤務」のみが存在する場合、 DB への永続化が不要であるため空リストが返却されること（C1
     * 早期リターン）を検証します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>予定休: 4件</li>
     *   <li>通常勤務（集計対象外区分）: 5件</li>
     *   <li>当日休・半休・遅延: 0件</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>{@code irregularCount == 0} が成立し、空リストが返却されること</li>
     * </ul>
     */
    @Test
    @DisplayName("C1分岐: 予定休のみ存在し不定期区分（当欠・半休・遅延）が0件の場合、登録不要として空リストを返却すること")
    void shouldReturnEmptyListWhenOnlyScheduledHolidaysExist() {
      // [Given]
      List<HrmosDailyWorkOutput> outputs = List.of(
          createDailyOutput("2026-04-01", "予定休"),
          createDailyOutput("2026-04-02", "予定休"),
          createDailyOutput("2026-04-03", "通常勤務"),
          createDailyOutput("2026-04-04", "予定休")
      );

      // [When]
      List<SummaryItem> actual = aggregator.aggregate(outputs, today);

      // [Then]
      assertThat(actual).isEmpty();
    }
  }

  @Nested
  @DisplayName("集計ロジックおよび通知フラグ判定シナリオ")
  class AggregationLogicScenario {

    /**
     * 【テスト観点 / 責務】 全ての集計対象区分（予定休、当日休、午前半休、午後半休、遅延）が存在する場合の 集計値、部分一致集計、および通知フラグ（予定休="1",
     * その他="0"）の正確性を包括的に検証します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>予定休: 2件</li>
     *   <li>当日休: 1件</li>
     *   <li>午前半休: 1件</li>
     *   <li>午後半休: 2件</li>
     *   <li>遅延: 3件</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>5種類のサマリ項目が生成されること</li>
     *   <li>予定休（SEG_SCHEDULED_HOLIDAY: 2）の {@code count} が 2、{@code msgSend} が "1" であること</li>
     *   <li>当日休（SEG_UNSCHEDULED_HOLIDAY: 5）の {@code count} が 1、{@code msgSend} が "0" であること</li>
     *   <li>午前半休（SEG_AM_HALF_HOLIDAY: 6）の {@code count} が 1、{@code msgSend} が "0" であること</li>
     *   <li>午後半休（SEG_PM_HALF_HOLIDAY: 34）の {@code count} が 2、{@code msgSend} が "0" であること</li>
     *   <li>遅延（SEG_DELAY: 35）の {@code count} が 3、{@code msgSend} が "0" であること</li>
     * </ul>
     */
    @Test
    @DisplayName("正常系: 全不定期区分および予定休が存在する場合、正確な件数と規定のmsg_sendフラグで集計されること")
    void shouldAggregateAllTargetSegmentsWithCorrectMsgSendFlags() {
      // [Given]
      List<HrmosDailyWorkOutput> outputs = List.of(
          // 予定休: 2件
          createDailyOutput("2026-04-01", "予定休"),
          createDailyOutput("2026-04-02", "予定休"),
          // 当日休: 1件
          createDailyOutput("2026-04-03", "当日休"),
          // 今前半休: 1件
          createDailyOutput("2026-04-06", "午前半休"),
          // 今後半休: 2件
          createDailyOutput("2026-04-07", "午後半休"),
          createDailyOutput("2026-04-08", "午後半休"),
          // 遅延: 3件
          createDailyOutput("2026-04-09", "遅延"),
          createDailyOutput("2026-04-10", "遅延"),
          createDailyOutput("2026-04-13", "遅延")
      );

      // [When]
      List<SummaryItem> actual = aggregator.aggregate(outputs, today);

      // [Then]
      assertThat(actual).hasSize(5);

      Map<Integer, SummaryItem> resultMap = actual.stream()
          .collect(Collectors.toMap(SummaryItem::segmentId, item -> item));

      // 1. 予定休の検証 (msgSend は "1")
      SummaryItem scheduled = resultMap.get(MonthlyAttendanceAggregator.SEG_SCHEDULED_HOLIDAY);
      assertThat(scheduled).isNotNull();
      assertThat(scheduled.segmentTitle()).isEqualTo("予定休");
      assertThat(scheduled.count()).isEqualTo(2);
      assertThat(scheduled.msgSend()).isEqualTo("1");

      // 2. 当日休の検証 (msgSend は "0")
      SummaryItem unscheduled = resultMap.get(MonthlyAttendanceAggregator.SEG_UNSCHEDULED_HOLIDAY);
      assertThat(unscheduled).isNotNull();
      assertThat(unscheduled.segmentTitle()).isEqualTo("当日休");
      assertThat(unscheduled.count()).isEqualTo(1);
      assertThat(unscheduled.msgSend()).isEqualTo("0");

      // 3. 今前半休の検証 (msgSend は "0")
      SummaryItem amHalf = resultMap.get(MonthlyAttendanceAggregator.SEG_AM_HALF_HOLIDAY);
      assertThat(amHalf).isNotNull();
      assertThat(amHalf.segmentTitle()).isEqualTo("午前半休");
      assertThat(amHalf.count()).isEqualTo(1);
      assertThat(amHalf.msgSend()).isEqualTo("0");

      // 4. 今後半休の検証 (msgSend は "0")
      SummaryItem pmHalf = resultMap.get(MonthlyAttendanceAggregator.SEG_PM_HALF_HOLIDAY);
      assertThat(pmHalf).isNotNull();
      assertThat(pmHalf.segmentTitle()).isEqualTo("午後半休");
      assertThat(pmHalf.count()).isEqualTo(2);
      assertThat(pmHalf.msgSend()).isEqualTo("0");

      // 5. 遅延の検証 (msgSend は "0")
      SummaryItem delay = resultMap.get(MonthlyAttendanceAggregator.SEG_DELAY);
      assertThat(delay).isNotNull();
      assertThat(delay.segmentTitle()).isEqualTo("遅延");
      assertThat(delay.count()).isEqualTo(3);
      assertThat(delay.msgSend()).isEqualTo("0");
    }

    /**
     * 【テスト観点 / 責務】 勤務区分タイトルが「予定休（法定）」「当日休（私用）」のように接尾辞・接頭辞を含む場合でも、
     * 部分一致（{@code contains}）によって意図した区分として正しくカウントされることを検証します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>"法定予定休" 1件</li>
     *   <li>"当日休（体調不良）" 1件</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>部分一致により予定休と当日休がそれぞれ 1件として抽出されること</li>
     * </ul>
     */
    @Test
    @DisplayName("正常系: 勤務区分名が部分一致（接頭辞・接尾辞あり）の場合でも正しく集計されること")
    void shouldAggregateMatchingSegmentTitlesWithPartialMatch() {
      // [Given]
      List<HrmosDailyWorkOutput> outputs = List.of(
          createDailyOutput("2026-04-01", "法定予定休"),
          createDailyOutput("2026-04-02", "当日休（体調不良）")
      );

      // [When]
      List<SummaryItem> actual = aggregator.aggregate(outputs, today);

      // [Then]
      assertThat(actual).hasSize(2);

      Map<Integer, SummaryItem> resultMap = actual.stream()
          .collect(Collectors.toMap(SummaryItem::segmentId, item -> item));

      assertThat(resultMap.get(MonthlyAttendanceAggregator.SEG_SCHEDULED_HOLIDAY).count())
          .isEqualTo(1);
      assertThat(resultMap.get(MonthlyAttendanceAggregator.SEG_UNSCHEDULED_HOLIDAY).count())
          .isEqualTo(1);
    }

    /**
     * 【テスト観点 / 責務】 発生していない勤務区分（件数が0件）がサマリ項目リスト（{@code items}）に含まれないこと（{@code count > 0} 分岐）を検証します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>"遅延" のみ 2件存在し、予定休、当日休、半休は0件であること</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>結果リストには遅延（SEG_DELAY: 35）の 1件のみが含まれ、0件の区分は追加されないこと</li>
     * </ul>
     */
    @Test
    @DisplayName("C1分岐: 件数が0件の勤務区分項目はサマリ結果リストに追加されないこと")
    void shouldNotIncludeSegmentsWithZeroCountInResultList() {
      // [Given]
      List<HrmosDailyWorkOutput> outputs = List.of(
          createDailyOutput("2026-04-01", "遅延"),
          createDailyOutput("2026-04-02", "遅延")
      );

      // [When]
      List<SummaryItem> actual = aggregator.aggregate(outputs, today);

      // [Then]
      assertThat(actual).hasSize(1);
      SummaryItem item = actual.getFirst();
      assertThat(item.segmentId()).isEqualTo(MonthlyAttendanceAggregator.SEG_DELAY);
      assertThat(item.count()).isEqualTo(2);
      assertThat(item.msgSend()).isEqualTo("0");
    }
  }
}
