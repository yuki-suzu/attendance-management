package com.computer_rescuer.attendance_management.application.interactor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.computer_rescuer.attendance_management.adapter.out.exception.ExternalIntegrationException;
import com.computer_rescuer.attendance_management.adapter.out.hrmos.client.HrmosAuthApi;
import com.computer_rescuer.attendance_management.adapter.out.hrmos.client.HrmosWorkOutputApi;
import com.computer_rescuer.attendance_management.adapter.out.hrmos.model.HrmosDailyWorkOutput;
import com.computer_rescuer.attendance_management.adapter.out.kafka.dto.AttendanceIrregularityEvent;
import com.computer_rescuer.attendance_management.adapter.out.kafka.support.AttendanceIrregularityEventBuilder;
import com.computer_rescuer.attendance_management.application.port.out.FetchEmployeeDepartmentPort;
import com.computer_rescuer.attendance_management.application.port.out.FetchEmployeePort;
import com.computer_rescuer.attendance_management.application.port.out.MonthlyAttendanceSummaryRepositoryPort;
import com.computer_rescuer.attendance_management.application.port.out.MonthlyAttendanceSummaryRepositoryPort.PersistedSummary;
import com.computer_rescuer.attendance_management.application.port.out.MonthlyAttendanceSummaryRepositoryPort.SummaryItem;
import com.computer_rescuer.attendance_management.application.port.out.NotifyMonthlySummaryPort;
import com.computer_rescuer.attendance_management.application.service.MonthlyAttendanceAggregator;
import com.computer_rescuer.attendance_management.domain.model.Employee;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link MonthlyAttendanceSummaryInteractor} の単体テストクラス。
 * <p>
 * 月次勤怠サマリ集計ユースケースの責務である以下の処理フローを網羅的に検証します:
 * <ul>
 *   <li>HRMOS API を用いた全従業員の日次勤怠実績取得とメモリ保護下での逐次 UPSERT</li>
 *   <li>特定従業員の外部 API 障害時におけるフォールバック（個別エラー捕捉と他従業員処理の継続）</li>
 *   <li>未送信サマリ（msg_send = '0'）の有無に応じた早期リターン判定</li>
 *   <li>マスタ補完および Kafka 通知イベント（{@link AttendanceIrregularityEvent}）の発行</li>
 *   <li>通知完了後の送信フラグ更新（{@code markAsSent}）</li>
 *   <li>上位例外発生時におけるトランザクションロールバック整合性</li>
 * </ul>
 * 本テストは Spring コンテキストを起動せず、Mockito による高速かつ決定論的な単体テストとして実行されます。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MonthlyAttendanceSummaryInteractor 単体テスト")
class MonthlyAttendanceSummaryInteractorTest {

  @Mock
  private HrmosAuthApi authApi;

  @Mock
  private HrmosWorkOutputApi workOutputApi;

  @Mock
  private FetchEmployeePort fetchEmployeePort;

  @Mock
  private FetchEmployeeDepartmentPort fetchEmployeeDepartmentPort;

  @Mock
  private MonthlyAttendanceSummaryRepositoryPort summaryRepositoryPort;

  @Mock
  private NotifyMonthlySummaryPort notifyMonthlySummaryPort;

  @Mock
  private MonthlyAttendanceAggregator aggregator;

  @Mock
  private AttendanceIrregularityEventBuilder eventBuilder;

  @InjectMocks
  private MonthlyAttendanceSummaryInteractor interactor;

  private final YearMonth targetMonth = YearMonth.of(2026, 4);
  private final String procMonth = "2026-04";
  private final String dummyToken = "test-hrmos-token";

  /**
   * テスト用の従業員ドメインモデルインスタンスを生成するヘルパーメソッド。
   *
   * @param id             従業員ID
   * @param employeeNumber 社員番号
   * @return 構築された {@link Employee}
   */
  private Employee createEmployee(Integer id, String employeeNumber) {
    return new Employee(
        id,
        employeeNumber,
        "山田",
        "太郎",
        "yamada@example.com",
        1,
        1,
        1
    );
  }

  @Nested
  @DisplayName("正常系シナリオ")
  class SuccessScenario {

    /**
     * 【テスト観点 / 責務】 正常系全工程の検証。全従業員の勤怠取得、集計、DB UPSERT、差分検知、 マスタ補完、Kafka
     * 通知、および送信完了更新が一連のフローとして正常に呼び出されることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>有効な認証トークンが取得できること</li>
     *   <li>従業員マスタから2名（EMP001, EMP002）が取得できること</li>
     *   <li>両名の勤怠集計結果が1件以上存在し、UPSERT 対象となること</li>
     *   <li>EMP001 のみが未送信差分（msg_send = '0'）として抽出されること</li>
     *   <li>Kafka イベントが正常に生成され、送信完了フラグが更新されること</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>両従業員に対して {@code upsertSummaries} が呼び出されること</li>
     *   <li>未送信の EMP001 に対してのみサマリ取得、部門取得、イベント生成、Kafka 送信が行われること</li>
     *   <li>最後に EMP001 の送信完了フラグ更新（{@code markAsSent}）が呼び出されること</li>
     * </ul>
     */
    @Test
    @DisplayName("正常系: 差分が検知された場合、集計・UPSERT後にKafka通知を発行し送信完了更新を実行すること")
    void shouldExecuteMonthlySummaryAndDispatchKafkaEventSuccessfully() {
      // [Given]
      Employee emp1 = createEmployee(101, "EMP001");
      Employee emp2 = createEmployee(102, "EMP002");
      List<Employee> employees = List.of(emp1, emp2);

      when(authApi.fetchToken()).thenReturn(dummyToken);
      when(fetchEmployeePort.fetchAll()).thenReturn(employees);

      HrmosDailyWorkOutput mockOutput1 = mock(HrmosDailyWorkOutput.class);
      HrmosDailyWorkOutput mockOutput2 = mock(HrmosDailyWorkOutput.class);
      when(workOutputApi.fetchMonthlyWorkOutputsByUser(dummyToken, procMonth, 101))
          .thenReturn(List.of(mockOutput1));
      when(workOutputApi.fetchMonthlyWorkOutputsByUser(dummyToken, procMonth, 102))
          .thenReturn(List.of(mockOutput2));

      SummaryItem item1 = new SummaryItem(2, "予定休", 8, "1");
      SummaryItem item2 = new SummaryItem(5, "当日休", 1, "0");
      when(aggregator.aggregate(eq(List.of(mockOutput1)), any(LocalDate.class)))
          .thenReturn(List.of(item1));
      when(aggregator.aggregate(eq(List.of(mockOutput2)), any(LocalDate.class)))
          .thenReturn(List.of(item2));

      List<String> unsentEmpNumbers = List.of("EMP001");
      when(summaryRepositoryPort.findEmployeeNumbersWithUnsentSummary(procMonth))
          .thenReturn(unsentEmpNumbers);

      PersistedSummary persistedSummary = new PersistedSummary(
          procMonth, "EMP001", 5, "当日休", 1, "0");
      List<PersistedSummary> persistedSummaries = List.of(persistedSummary);
      when(summaryRepositoryPort.findSummariesByEmployeeNumbers(procMonth, unsentEmpNumbers))
          .thenReturn(persistedSummaries);

      Map<Integer, String> deptMap = Map.of(101, "開発部");
      when(fetchEmployeeDepartmentPort.fetchDepartmentMapByUserIds(List.of(101)))
          .thenReturn(deptMap);

      AttendanceIrregularityEvent mockEvent = mock(AttendanceIrregularityEvent.class);
      when(eventBuilder.buildEvent(eq(procMonth), eq(persistedSummaries), any(), eq(deptMap)))
          .thenReturn(mockEvent);

      // [When]
      interactor.execute(targetMonth);

      // [Then]
      verify(summaryRepositoryPort).upsertSummaries(procMonth, "EMP001", List.of(item1));
      verify(summaryRepositoryPort).upsertSummaries(procMonth, "EMP002", List.of(item2));
      verify(notifyMonthlySummaryPort).sendMonthlySummary(mockEvent);
      verify(summaryRepositoryPort).markAsSent(procMonth, unsentEmpNumbers);
    }
  }

  @Nested
  @DisplayName("境界値および条件分岐シナリオ")
  class BoundaryAndBranchScenario {

    /**
     * 【テスト観点 / 責務】 差分未検知時の早期リターン分岐（C1 カバレッジ）の検証。 全従業員の集計・UPSERT
     * 後、未送信対象者（{@code unsentEmpNumbers}）が空の場合に、 後続の Kafka 通知や送信完了更新を行わずに安全に処理が終了することを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>従業員1名（EMP001）が存在し、UPSERT が行われること</li>
     *   <li>{@code findEmployeeNumbersWithUnsentSummary} が空リストを返却すること</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>UPSERT までは実行されること</li>
     *   <li>Kafka 通知ポートおよび送信完了更新ポートは一切呼び出されないこと</li>
     * </ul>
     */
    @Test
    @DisplayName("境界値: 未送信従業員が存在しない場合、Kafka通知および送信完了更新を行わず早期リターンすること")
    void shouldEarlyReturnWhenNoUnsentSummaryExists() {
      // [Given]
      Employee emp1 = createEmployee(101, "EMP001");
      when(authApi.fetchToken()).thenReturn(dummyToken);
      when(fetchEmployeePort.fetchAll()).thenReturn(List.of(emp1));

      SummaryItem item = new SummaryItem(2, "予定休", 8, "1");
      when(workOutputApi.fetchMonthlyWorkOutputsByUser(dummyToken, procMonth, 101))
          .thenReturn(Collections.emptyList());
      when(aggregator.aggregate(anyList(), any(LocalDate.class)))
          .thenReturn(List.of(item));

      when(summaryRepositoryPort.findEmployeeNumbersWithUnsentSummary(procMonth))
          .thenReturn(Collections.emptyList());

      // [When]
      interactor.execute(targetMonth);

      // [Then]
      verify(summaryRepositoryPort).upsertSummaries(procMonth, "EMP001", List.of(item));
      verify(summaryRepositoryPort).findEmployeeNumbersWithUnsentSummary(procMonth);
      verifyNoInteractions(fetchEmployeeDepartmentPort);
      verifyNoInteractions(eventBuilder);
      verifyNoInteractions(notifyMonthlySummaryPort);
      verify(summaryRepositoryPort, never()).markAsSent(anyString(), anyList());
    }

    /**
     * 【テスト観点 / 責務】 集計結果が空の従業員に対する UPSERT スキップ分岐（C1 カバレッジ）の検証。
     * {@link MonthlyAttendanceAggregator#aggregate} が空リスト（予定休のみなど）を返却した場合、 当該従業員の
     * {@code upsertSummaries} 呼び出しがスキップされることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>従業員1名（EMP001）が存在すること</li>
     *   <li>{@code aggregator.aggregate} が空リストを返却すること</li>
     *   <li>未送信従業員も存在しないこと</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>{@code upsertSummaries} が一度も呼び出されないこと</li>
     * </ul>
     */
    @Test
    @DisplayName("C1分岐: 集計サマリ項目が空（登録対象外）の場合、UPSERT処理をスキップすること")
    void shouldSkipUpsertWhenAggregatedItemsIsEmpty() {
      // [Given]
      Employee emp1 = createEmployee(101, "EMP001");
      when(authApi.fetchToken()).thenReturn(dummyToken);
      when(fetchEmployeePort.fetchAll()).thenReturn(List.of(emp1));
      when(workOutputApi.fetchMonthlyWorkOutputsByUser(dummyToken, procMonth, 101))
          .thenReturn(Collections.emptyList());
      when(aggregator.aggregate(anyList(), any(LocalDate.class)))
          .thenReturn(Collections.emptyList());
      when(summaryRepositoryPort.findEmployeeNumbersWithUnsentSummary(procMonth))
          .thenReturn(Collections.emptyList());

      // [When]
      interactor.execute(targetMonth);

      // [Then]
      verify(summaryRepositoryPort, never()).upsertSummaries(anyString(), anyString(), anyList());
      verify(summaryRepositoryPort).findEmployeeNumbersWithUnsentSummary(procMonth);
    }

    /**
     * 【テスト観点 / 責務】 従業員リストが0件の場合の境界値検証。 ループ処理が1度も実行されず、未送信判定を経て正常終了することを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>{@code fetchEmployeePort.fetchAll()} が空リストを返却すること</li>
     *   <li>{@code findEmployeeNumbersWithUnsentSummary} が空リストを返却すること</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>勤怠取得 API、集計、UPSERT が一切実行されないこと</li>
     * </ul>
     */
    @Test
    @DisplayName("境界値: 従業員マスタが0件の場合、集計ループを実行せず安全に早期リターンすること")
    void shouldHandleEmptyEmployeeListGracefully() {
      // [Given]
      when(authApi.fetchToken()).thenReturn(dummyToken);
      when(fetchEmployeePort.fetchAll()).thenReturn(Collections.emptyList());
      when(summaryRepositoryPort.findEmployeeNumbersWithUnsentSummary(procMonth))
          .thenReturn(Collections.emptyList());

      // [When]
      interactor.execute(targetMonth);

      // [Then]
      verifyNoInteractions(workOutputApi);
      verifyNoInteractions(aggregator);
      verify(summaryRepositoryPort, never()).upsertSummaries(anyString(), anyString(), anyList());
      verify(summaryRepositoryPort).findEmployeeNumbersWithUnsentSummary(procMonth);
    }

    /**
     * 【テスト観点 / 責務】 社員番号重複時のマージ関数 {@code (e1, e2) -> e1} および マスタ未存在社員番号の null 除外フィルタリング（C1
     * カバレッジ）の検証。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>社員番号 "EMP001" を持つ従業員が2件重複して取得されること（先勝ち検証）</li>
     *   <li>未送信従業員リストに、マスタに存在しない "UNKNOWN_EMP" が含まれていること</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>部門取得ポートへ渡されるユーザーIDリストから null が除外され、有効な ID のみが渡されること</li>
     *   <li>マージ関数によって先勝ちの従業員インスタンスがマップに採用されること</li>
     * </ul>
     */
    @Test
    @DisplayName("C1分岐: 重複社員番号のマージおよびマスタ未存在社員番号のnull除外が正常に機能すること")
    void shouldHandleDuplicateEmployeeNumbersAndNullMappingCorrectly() {
      // [Given]
      Employee empFirst = new Employee(101, "EMP001", "山田", "太郎", "y@ex.com", 1, 1, 1);
      Employee empDuplicate = new Employee(999, "EMP001", "山田", "次郎", "y2@ex.com", 1, 1, 1);
      List<Employee> employees = List.of(empFirst, empDuplicate);

      when(authApi.fetchToken()).thenReturn(dummyToken);
      when(fetchEmployeePort.fetchAll()).thenReturn(employees);

      when(workOutputApi.fetchMonthlyWorkOutputsByUser(anyString(), anyString(), anyInt()))
          .thenReturn(Collections.emptyList());
      when(aggregator.aggregate(anyList(), any(LocalDate.class)))
          .thenReturn(Collections.emptyList());

      // 未送信リストにマスタに存在しない社員番号を含める
      List<String> unsentEmpNumbers = List.of("EMP001", "UNKNOWN_EMP");
      when(summaryRepositoryPort.findEmployeeNumbersWithUnsentSummary(procMonth))
          .thenReturn(unsentEmpNumbers);

      PersistedSummary summary = new PersistedSummary(procMonth, "EMP001", 5, "当日休", 1, "0");
      when(summaryRepositoryPort.findSummariesByEmployeeNumbers(procMonth, unsentEmpNumbers))
          .thenReturn(List.of(summary));

      // ユーザーID 101 のみが部門取得に渡るはず（999は重複で排除され、UNKNOWN_EMPはnullで除外）
      when(fetchEmployeeDepartmentPort.fetchDepartmentMapByUserIds(List.of(101)))
          .thenReturn(Map.of(101, "開発部"));

      AttendanceIrregularityEvent mockEvent = mock(AttendanceIrregularityEvent.class);
      when(eventBuilder.buildEvent(eq(procMonth), anyList(), any(), any()))
          .thenReturn(mockEvent);

      // [When]
      interactor.execute(targetMonth);

      // [Then]
      ArgumentCaptor<List<Integer>> idCaptor = ArgumentCaptor.forClass(List.class);
      verify(fetchEmployeeDepartmentPort).fetchDepartmentMapByUserIds(idCaptor.capture());
      assertThat(idCaptor.getValue()).containsExactly(101);

      verify(notifyMonthlySummaryPort).sendMonthlySummary(mockEvent);
      verify(summaryRepositoryPort).markAsSent(procMonth, unsentEmpNumbers);
    }
  }

  @Nested
  @DisplayName("異常系および耐障害性（フォールバック）シナリオ")
  class ExceptionAndFaultToleranceScenario {

    /**
     * 【テスト観点 / 責務】 従業員個別集計エラー時の耐障害性（フォールバック）検証。 特定の従業員の外部 API 通信（または集計）で例外が発生した場合でも、 当該従業員の処理は
     * catch されてスキップされ、後続の従業員の処理が中断されずに継続することを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>従業員2名（EMP001: 障害発生, EMP002: 正常）が存在すること</li>
     *   <li>EMP001 に対する {@code fetchMonthlyWorkOutputsByUser} が {@link RuntimeException} をスローすること</li>
     *   <li>EMP002 は正常に取得・集計され、差分対象となること</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>Interactor 全体としては例外がスローされず正常終了すること</li>
     *   <li>EMP001 の UPSERT はスキップされるが、EMP002 の UPSERT、Kafka 通知、送信完了更新は完遂されること</li>
     * </ul>
     */
    @Test
    @DisplayName("耐障害性: 特定従業員の外部API取得が失敗しても、エラーを隔離して他従業員の処理を継続すること")
    void shouldContinueProcessingOtherEmployeesWhenOneEmployeeFails() {
      // [Given]
      Employee empFailed = createEmployee(101, "EMP001");
      Employee empSuccess = createEmployee(102, "EMP002");
      List<Employee> employees = List.of(empFailed, empSuccess);

      when(authApi.fetchToken()).thenReturn(dummyToken);
      when(fetchEmployeePort.fetchAll()).thenReturn(employees);

      // EMP001 は例外発生
      when(workOutputApi.fetchMonthlyWorkOutputsByUser(dummyToken, procMonth, 101))
          .thenThrow(new ExternalIntegrationException("HRMOS API 通信タイムアウト"));

      // EMP002 は正常
      HrmosDailyWorkOutput mockOutput = mock(HrmosDailyWorkOutput.class);
      when(workOutputApi.fetchMonthlyWorkOutputsByUser(dummyToken, procMonth, 102))
          .thenReturn(List.of(mockOutput));
      SummaryItem itemSuccess = new SummaryItem(5, "当日休", 1, "0");
      when(aggregator.aggregate(eq(List.of(mockOutput)), any(LocalDate.class)))
          .thenReturn(List.of(itemSuccess));

      List<String> unsentEmpNumbers = List.of("EMP002");
      when(summaryRepositoryPort.findEmployeeNumbersWithUnsentSummary(procMonth))
          .thenReturn(unsentEmpNumbers);
      when(summaryRepositoryPort.findSummariesByEmployeeNumbers(procMonth, unsentEmpNumbers))
          .thenReturn(Collections.emptyList());
      when(fetchEmployeeDepartmentPort.fetchDepartmentMapByUserIds(List.of(102)))
          .thenReturn(Map.of(102, "総務部"));

      AttendanceIrregularityEvent mockEvent = mock(AttendanceIrregularityEvent.class);
      when(eventBuilder.buildEvent(eq(procMonth), anyList(), any(), any()))
          .thenReturn(mockEvent);

      // [When]
      interactor.execute(targetMonth);

      // [Then]
      // EMP001 は UPSERT されない
      verify(summaryRepositoryPort, never()).upsertSummaries(eq(procMonth), eq("EMP001"),
          anyList());
      // EMP002 は正常に UPSERT される
      verify(summaryRepositoryPort).upsertSummaries(procMonth, "EMP002", List.of(itemSuccess));
      // 後続の通知とステータス更新が EMP002 に対して実行される
      verify(notifyMonthlySummaryPort).sendMonthlySummary(mockEvent);
      verify(summaryRepositoryPort).markAsSent(procMonth, unsentEmpNumbers);
    }

    /**
     * 【テスト観点 / 責務】 初期化処理における致命的例外のロールバック伝播検証。 トークン取得（{@link HrmosAuthApi#fetchToken()}）で例外が発生した場合、
     * 後続処理を一切行わずに例外がスローされ、トランザクションがロールバックされることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>{@code authApi.fetchToken()} が {@link ExternalIntegrationException} をスローすること</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>{@code assertThatThrownBy} により同例外が送出されること</li>
     *   <li>従業員取得や DB 永続化等の後続処理が一切呼び出されないこと</li>
     * </ul>
     */
    @Test
    @DisplayName("異常系: 認証トークン取得に失敗した場合、例外をそのままスローし後続処理を中断すること")
    void shouldPropagateExceptionWhenAuthFails() {
      // [Given]
      when(authApi.fetchToken())
          .thenThrow(new ExternalIntegrationException("認証サーバー障害"));

      // [When & Then]
      assertThatThrownBy(() -> interactor.execute(targetMonth))
          .isInstanceOf(ExternalIntegrationException.class)
          .hasMessage("認証サーバー障害");

      verifyNoInteractions(fetchEmployeePort);
      verifyNoInteractions(workOutputApi);
      verifyNoInteractions(aggregator);
      verifyNoInteractions(summaryRepositoryPort);
      verifyNoInteractions(notifyMonthlySummaryPort);
    }

    /**
     * 【テスト観点 / 責務】 Kafka メッセージディスパッチ失敗時の例外伝播検証。 {@link NotifyMonthlySummaryPort#sendMonthlySummary}
     * でブローカー障害等の例外が発生した場合、 後続の {@code markAsSent}（送信完了フラグ更新）を実行させずに例外をスローすることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>勤怠集計およびイベント生成まで正常に完了すること</li>
     *   <li>{@code notifyMonthlySummaryPort.sendMonthlySummary} が {@link RuntimeException} をスローすること</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>{@code assertThatThrownBy} により同例外が送出されること</li>
     *   <li>{@code summaryRepositoryPort.markAsSent} は決して呼び出されないこと</li>
     * </ul>
     */
    @Test
    @DisplayName("異常系: Kafkaディスパッチに失敗した場合、例外がスローされ送信完了更新が行われないこと")
    void shouldNotMarkAsSentWhenKafkaDispatchFails() {
      // [Given]
      Employee emp = createEmployee(101, "EMP001");
      when(authApi.fetchToken()).thenReturn(dummyToken);
      when(fetchEmployeePort.fetchAll()).thenReturn(List.of(emp));
      when(workOutputApi.fetchMonthlyWorkOutputsByUser(dummyToken, procMonth, 101))
          .thenReturn(Collections.emptyList());
      when(aggregator.aggregate(anyList(), any(LocalDate.class)))
          .thenReturn(Collections.emptyList());

      List<String> unsentEmpNumbers = List.of("EMP001");
      when(summaryRepositoryPort.findEmployeeNumbersWithUnsentSummary(procMonth))
          .thenReturn(unsentEmpNumbers);
      when(summaryRepositoryPort.findSummariesByEmployeeNumbers(procMonth, unsentEmpNumbers))
          .thenReturn(Collections.emptyList());
      when(fetchEmployeeDepartmentPort.fetchDepartmentMapByUserIds(List.of(101)))
          .thenReturn(Map.of(101, "開発部"));

      AttendanceIrregularityEvent mockEvent = mock(AttendanceIrregularityEvent.class);
      when(eventBuilder.buildEvent(eq(procMonth), anyList(), any(), any()))
          .thenReturn(mockEvent);

      // Kafka 送信で障害発生
      doThrow(new RuntimeException("Kafka Broker Unavailable"))
          .when(notifyMonthlySummaryPort).sendMonthlySummary(mockEvent);

      // [When & Then]
      assertThatThrownBy(() -> interactor.execute(targetMonth))
          .isInstanceOf(RuntimeException.class)
          .hasMessage("Kafka Broker Unavailable");

      verify(notifyMonthlySummaryPort).sendMonthlySummary(mockEvent);
      // 送信完了フラグは更新されてはならない
      verify(summaryRepositoryPort, never()).markAsSent(anyString(), anyList());
    }
  }
}
