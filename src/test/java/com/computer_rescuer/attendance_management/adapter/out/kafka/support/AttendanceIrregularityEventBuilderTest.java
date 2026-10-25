package com.computer_rescuer.attendance_management.adapter.out.kafka.support;

import static com.computer_rescuer.attendance_management.application.service.MonthlyAttendanceAggregator.SEG_AM_HALF_HOLIDAY;
import static com.computer_rescuer.attendance_management.application.service.MonthlyAttendanceAggregator.SEG_DELAY;
import static com.computer_rescuer.attendance_management.application.service.MonthlyAttendanceAggregator.SEG_PM_HALF_HOLIDAY;
import static com.computer_rescuer.attendance_management.application.service.MonthlyAttendanceAggregator.SEG_SCHEDULED_HOLIDAY;
import static com.computer_rescuer.attendance_management.application.service.MonthlyAttendanceAggregator.SEG_UNSCHEDULED_HOLIDAY;
import static org.assertj.core.api.Assertions.assertThat;

import com.computer_rescuer.attendance_management.adapter.out.kafka.dto.AttendanceIrregularityEvent;
import com.computer_rescuer.attendance_management.adapter.out.kafka.dto.AttendanceIrregularityEvent.EmployeeIrregularitySummary;
import com.computer_rescuer.attendance_management.application.port.out.MonthlyAttendanceSummaryRepositoryPort.PersistedSummary;
import com.computer_rescuer.attendance_management.domain.model.Employee;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link AttendanceIrregularityEventBuilder} の単体テストクラス。
 * <p>
 * データベースから取得した月次サマリ永続化レコード群（{@link PersistedSummary}）を、 Kafka 送信用イベント
 * DTO（{@link AttendanceIrregularityEvent}）へ集約・変換する以下の責務を網羅的に検証します:
 * <ul>
 *   <li>従業員番号（employeeNumber）単位のグルーピング処理</li>
 *   <li>各勤務区分 ID（予定休・当欠・午前半休・午後半休・遅延）の正確な振り分け集計</li>
 *   <li>午前半休（6）と午後半休（34）の「半日休暇日数」への合算集計ロジック</li>
 *   <li>従業員マスタ（Employee）および部門マスタ（deptMap）からの氏名・部門名の補完</li>
 *   <li>マスタ不在時の安全なフォールバック（氏名: "不明"、所属: "未所属"）</li>
 *   <li>対象外（未知）の勤務区分レコードが存在する場合の安全なスキップ（全条件分岐の網羅）</li>
 *   <li>サマリレコードが空データの場合の境界値ハンドリング</li>
 * </ul>
 * モック外部通信を行わず、純粋なデータ変換ロジックを高速かつ決定論的に検証します。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AttendanceIrregularityEventBuilder 単体テスト")
class AttendanceIrregularityEventBuilderTest {

  @InjectMocks
  private AttendanceIrregularityEventBuilder eventBuilder;

  private final String procMonth = "2026-04";

  /**
   * テスト用の従業員ドメインモデルを生成するヘルパーメソッド。
   *
   * @param id             従業員内部ID
   * @param employeeNumber 社員番号
   * @param lastName       姓
   * @param firstName      名
   * @return 構築された {@link Employee}
   */
  private Employee createEmployee(Integer id, String employeeNumber, String lastName,
      String firstName) {
    return new Employee(
        id,
        employeeNumber,
        lastName,
        firstName,
        "test@example.com",
        1,
        1,
        1
    );
  }

  @Nested
  @DisplayName("正常系および集計振り分けシナリオ")
  class AggregationSuccessScenario {

    /**
     * 【テスト観点 / 責務】 全種類の勤務区分（予定休、当欠、午前半休、午後半休、遅延）が存在する場合の集約検証。 特に「午前半休」と「午後半休」が合算されて
     * `halfHolidayCount` に集計されること、 および従業員マスタと部門名が正しく補完されて DTO が生成されることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>処理対象月: "2026-04"</li>
     *   <li>従業員1名（EMP001: 徳川 家康, 内部ID: 101）</li>
     *   <li>部門マップ: 101 -> "システム開発部"</li>
     *   <li>サマリレコード:
     *     <ul>
     *       <li>予定休 (ID: 2) -> 8件</li>
     *       <li>当日休 (ID: 5) -> 2件</li>
     *       <li>午前半休 (ID: 6) -> 1件</li>
     *       <li>午後半休 (ID: 34) -> 2件</li>
     *       <li>遅延 (ID: 35) -> 3件</li>
     *     </ul>
     *   </li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>生成されたイベントの `procMonth` が "2026-04" であること</li>
     *   <li>従業員集約結果が1件存在し、社員番号が "EMP001" であること</li>
     *   <li>氏名が "徳川 家康"、部門名が "システム開発部" であること</li>
     *   <li>予定休日数が 8、当日欠勤日数が 2、遅延回数が 3 であること</li>
     *   <li>半休回数が午前(1)と午後(2)の合算である 3 であること</li>
     * </ul>
     */
    @Test
    @DisplayName("正常系: 全対象区分が存在する場合、半休の合算を含めて正しく集計・マスタ補完が行われること")
    void shouldBuildEventWithAllSegmentTypesAndEnrichedDataSuccessfully() {
      // [Given]
      Employee emp = createEmployee(101, "EMP001", "徳川", "家康");
      Map<String, Employee> employeeMap = Map.of("EMP001", emp);
      Map<Integer, String> deptMap = Map.of(101, "システム開発部");

      List<PersistedSummary> summaries = List.of(
          new PersistedSummary(procMonth, "EMP001", SEG_SCHEDULED_HOLIDAY, "予定休", 8, "1"),
          new PersistedSummary(procMonth, "EMP001", SEG_UNSCHEDULED_HOLIDAY, "当日休", 2, "0"),
          new PersistedSummary(procMonth, "EMP001", SEG_AM_HALF_HOLIDAY, "午前半休", 1, "0"),
          new PersistedSummary(procMonth, "EMP001", SEG_PM_HALF_HOLIDAY, "午後半休", 2, "0"),
          new PersistedSummary(procMonth, "EMP001", SEG_DELAY, "遅延", 3, "0")
      );

      // [When]
      AttendanceIrregularityEvent actualEvent = eventBuilder.buildEvent(
          procMonth, summaries, employeeMap, deptMap);

      // [Then]
      assertThat(actualEvent).isNotNull();
      assertThat(actualEvent.procMonth()).isEqualTo(procMonth);
      assertThat(actualEvent.employees()).hasSize(1);

      EmployeeIrregularitySummary summary = actualEvent.employees().getFirst();
      assertThat(summary.employeeNumber()).isEqualTo("EMP001");
      assertThat(summary.departmentName()).isEqualTo("システム開発部");
      assertThat(summary.fullName()).isEqualTo("徳川 家康");
      assertThat(summary.scheduledHolidayCount()).isEqualTo(8);
      assertThat(summary.unscheduledHolidayCount()).isEqualTo(2);
      assertThat(summary.halfHolidayCount()).isEqualTo(3); // 1 (AM) + 2 (PM) = 3
      assertThat(summary.delayCount()).isEqualTo(3);
    }

    /**
     * 【テスト観点 / 責務】 複数従業員のサマリレコード群が渡された場合、従業員番号ごとに正しくグループ化され、 各人の集計結果が独立して保持されることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>EMP001（織田 信長）: 当日休 1件</li>
     *   <li>EMP002（豊臣 秀吉）: 遅延 2件</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>生成されたイベントの `employees` が 2件存在すること</li>
     *   <li>EMP001 と EMP002 の情報がそれぞれのデータで集計されていること</li>
     * </ul>
     */
    @Test
    @DisplayName("正常系: 複数従業員のサマリレコードが従業員ごとに正しくグルーピング・集計されること")
    void shouldGroupMultipleEmployeesCorrectly() {
      // [Given]
      Employee emp1 = createEmployee(101, "EMP001", "織田", "信長");
      Employee emp2 = createEmployee(102, "EMP002", "豊臣", "秀吉");
      Map<String, Employee> employeeMap = Map.of(
          "EMP001", emp1,
          "EMP002", emp2
      );
      Map<Integer, String> deptMap = Map.of(
          101, "第一営業部",
          102, "第二営業部"
      );

      List<PersistedSummary> summaries = List.of(
          new PersistedSummary(procMonth, "EMP001", SEG_UNSCHEDULED_HOLIDAY, "当日休", 1, "0"),
          new PersistedSummary(procMonth, "EMP002", SEG_DELAY, "遅延", 2, "0")
      );

      // [When]
      AttendanceIrregularityEvent actualEvent = eventBuilder.buildEvent(
          procMonth, summaries, employeeMap, deptMap);

      // [Then]
      assertThat(actualEvent.employees()).hasSize(2);

      Map<String, EmployeeIrregularitySummary> summaryMap = actualEvent.employees().stream()
          .collect(java.util.stream.Collectors.toMap(
              EmployeeIrregularitySummary::employeeNumber,
              s -> s
          ));

      EmployeeIrregularitySummary s1 = summaryMap.get("EMP001");
      assertThat(s1).isNotNull();
      assertThat(s1.fullName()).isEqualTo("織田 信長");
      assertThat(s1.departmentName()).isEqualTo("第一営業部");
      assertThat(s1.unscheduledHolidayCount()).isEqualTo(1);
      assertThat(s1.delayCount()).isZero();

      EmployeeIrregularitySummary s2 = summaryMap.get("EMP002");
      assertThat(s2).isNotNull();
      assertThat(s2.fullName()).isEqualTo("豊臣 秀吉");
      assertThat(s2.departmentName()).isEqualTo("第二営業部");
      assertThat(s2.unscheduledHolidayCount()).isZero();
      assertThat(s2.delayCount()).isEqualTo(2);
    }
  }

  @Nested
  @DisplayName("境界値および条件分岐・フォールバックシナリオ")
  class BoundaryAndBranchScenario {

    /**
     * 【テスト観点 / 責務】 従業員マスタマップに対象社員番号が存在しない場合（`emp == null`）の三項演算子分岐網羅。 氏名が "不明"、部門名が "未所属"
     * へフォールバックされることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>サマリレコードは社員番号 "EMP999" を保持する</li>
     *   <li>`employeeMap` は空マップ（該当従業員なし）</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>`fullName` が "不明" となること</li>
     *   <li>`departmentName` が "未所属" となること</li>
     * </ul>
     */
    @Test
    @DisplayName("C1分岐: 従業員マスタに存在しない場合、氏名が「不明」、部門名が「未所属」にフォールバックすること")
    void shouldFallbackToDefaultValuesWhenEmployeeNotFoundInMap() {
      // [Given]
      List<PersistedSummary> summaries = List.of(
          new PersistedSummary(procMonth, "EMP999", SEG_SCHEDULED_HOLIDAY, "予定休", 5, "1")
      );

      // [When]
      AttendanceIrregularityEvent actualEvent = eventBuilder.buildEvent(
          procMonth, summaries, Collections.emptyMap(), Collections.emptyMap());

      // [Then]
      assertThat(actualEvent.employees()).hasSize(1);
      EmployeeIrregularitySummary summary = actualEvent.employees().getFirst();
      assertThat(summary.employeeNumber()).isEqualTo("EMP999");
      assertThat(summary.fullName()).isEqualTo("不明");
      assertThat(summary.departmentName()).isEqualTo("未所属");
      assertThat(summary.scheduledHolidayCount()).isEqualTo(5);
    }

    /**
     * 【テスト観点 / 責務】 従業員マスタには存在するが、部門名マップ（`deptMap`）に当該従業員の内部IDが存在しない場合の分岐網羅。 `deptMap.getOrDefault`
     * により部門名が "未所属" へフォールバックされることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>従業員マスタに EMP001（内部ID: 101）が存在する</li>
     *   <li>`deptMap` は空マップ（内部ID 101 の部門名が未定義）</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>`fullName` は "徳川 家康" と正常に補完されること</li>
     *   <li>`departmentName` は "未所属" となること</li>
     * </ul>
     */
    @Test
    @DisplayName("C1分岐: 部門マスタに該当IDが存在しない場合、部門名が「未所属」にフォールバックすること")
    void shouldFallbackToDefaultDepartmentWhenDepartmentNotFoundInMap() {
      // [Given]
      Employee emp = createEmployee(101, "EMP001", "徳川", "家康");
      Map<String, Employee> employeeMap = Map.of("EMP001", emp);

      List<PersistedSummary> summaries = List.of(
          new PersistedSummary(procMonth, "EMP001", SEG_DELAY, "遅延", 1, "0")
      );

      // [When]
      AttendanceIrregularityEvent actualEvent = eventBuilder.buildEvent(
          procMonth, summaries, employeeMap, Collections.emptyMap()); // deptMap is empty

      // [Then]
      assertThat(actualEvent.employees()).hasSize(1);
      EmployeeIrregularitySummary summary = actualEvent.employees().getFirst();
      assertThat(summary.fullName()).isEqualTo("徳川 家康");
      assertThat(summary.departmentName()).isEqualTo("未所属");
      assertThat(summary.delayCount()).isEqualTo(1);
    }

    /**
     * 【テスト観点 / 責務】 集計対象外（未知）の勤務区分 ID が含まれる場合の分岐網羅（if-else の全否定ルート検証）。 対象外の勤務区分が安全に無視され、他のカウント値が 0
     * のまま正常に保持されることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>サマリレコードに集計対象外の `segmentId = 999`（例: 特別休暇等）が指定されていること</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>すべてのカウント（scheduled, unscheduled, half, delay）が 0 のまま返却されること</li>
     * </ul>
     */
    @Test
    @DisplayName("C1分岐: 対象外の勤務区分IDは無視され、各カウントが0のまま集計されること")
    void shouldIgnoreUnknownSegmentIdsWithoutAffectingOtherCounts() {
      // [Given]
      Employee emp = createEmployee(101, "EMP001", "徳川", "家康");
      Map<String, Employee> employeeMap = Map.of("EMP001", emp);
      Map<Integer, String> deptMap = Map.of(101, "システム開発部");

      int unknownSegmentId = 999;
      List<PersistedSummary> summaries = List.of(
          new PersistedSummary(procMonth, "EMP001", unknownSegmentId, "特別休暇", 3, "0")
      );

      // [When]
      AttendanceIrregularityEvent actualEvent = eventBuilder.buildEvent(
          procMonth, summaries, employeeMap, deptMap);

      // [Then]
      assertThat(actualEvent.employees()).hasSize(1);
      EmployeeIrregularitySummary summary = actualEvent.employees().getFirst();
      assertThat(summary.scheduledHolidayCount()).isZero();
      assertThat(summary.unscheduledHolidayCount()).isZero();
      assertThat(summary.halfHolidayCount()).isZero();
      assertThat(summary.delayCount()).isZero();
    }

    /**
     * 【テスト観点 / 責務】 サマリレコードリストが空の場合の境界値検証。 エラーを発生させずに空の従業員リストを持つイベントが返却されることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>`summaries` が空リスト（`Collections.emptyList()`）</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>`actualEvent.employees()` が空リストであること</li>
     *   <li>`actualEvent.procMonth()` が引数の対象月と一致すること</li>
     * </ul>
     */
    @Test
    @DisplayName("境界値: サマリレコードが空の場合、空の従業員リストを持つイベントを返却すること")
    void shouldReturnEventWithEmptyEmployeesWhenSummariesIsEmpty() {
      // [When]
      AttendanceIrregularityEvent actualEvent = eventBuilder.buildEvent(
          procMonth, Collections.emptyList(), Collections.emptyMap(), Collections.emptyMap());

      // [Then]
      assertThat(actualEvent).isNotNull();
      assertThat(actualEvent.procMonth()).isEqualTo(procMonth);
      assertThat(actualEvent.employees()).isEmpty();
    }
  }
}
