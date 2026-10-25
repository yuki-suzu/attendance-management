package com.computer_rescuer.attendance_management.application.interactor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.computer_rescuer.attendance_management.adapter.out.exception.ExternalIntegrationException;
import com.computer_rescuer.attendance_management.application.port.out.FetchStampLogPort;
import com.computer_rescuer.attendance_management.domain.model.StampLog;
import java.time.LocalDate;
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
 * {@link GetStampLogsInteractor} の単体テストクラス。
 * <p>
 * 打刻履歴取得ユースケース（{@link
 * com.computer_rescuer.attendance_management.application.port.in.GetStampLogsUseCase}）の実装として、
 * 以下の責務、境界条件、および例外ハンドリングを網羅的に検証します:
 * <ul>
 *   <li>日次打刻履歴取得（{@code getDailyLogs}）における引数の透過的委譲と取得結果の返却</li>
 *   <li>特定従業員の期間打刻履歴取得（{@code getUserLogs}）における社員番号および期間指定の透過的委譲</li>
 *   <li>出力ポート（{@link FetchStampLogPort}）から空データが返却された場合の安全なハンドリング</li>
 *   <li>出力ポートで外部連携例外等のシステム障害が発生した際の例外伝播</li>
 * </ul>
 * 本テストは Spring コンテキストを起動せず、純粋な Mockito 単体テストとして高速かつ決定論的に実行されます。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetStampLogsInteractor 単体テスト")
class GetStampLogsInteractorTest {

  @Mock
  private FetchStampLogPort fetchStampLogPort;

  @InjectMocks
  private GetStampLogsInteractor interactor;

  @Nested
  @DisplayName("全従業員の日次打刻履歴取得（getDailyLogs）シナリオ")
  class GetDailyLogsScenario {

    /**
     * 【テスト観点 / 責務】 正常系検証。指定された対象日付が出力ポート（{@link FetchStampLogPort#fetchDailyLogs(LocalDate)}）へ
     * 正確に引き渡され、取得された打刻ログリストがそのまま返却されることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>対象日付: {@code 2026-04-20}</li>
     *   <li>出力ポートが 1 件の打刻ログを含むリストを返却するようモック設定</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>返却されたリストのサイズが 1 件であり、モックされたログと一致すること</li>
     *   <li>{@link FetchStampLogPort#fetchDailyLogs(LocalDate)} が対象日を引数として実行されたこと</li>
     * </ul>
     */
    @Test
    @DisplayName("正常系: 指定日付の日次打刻ログが出力ポートから取得され、そのまま返却されること")
    void shouldReturnDailyLogsSuccessfully() {
      // [Given]
      LocalDate targetDate = LocalDate.of(2026, 4, 20);
      StampLog mockLog = mock(StampLog.class);
      List<StampLog> expectedLogs = List.of(mockLog);

      when(fetchStampLogPort.fetchDailyLogs(targetDate)).thenReturn(expectedLogs);

      // [When]
      List<StampLog> actual = interactor.getDailyLogs(targetDate);

      // [Then]
      assertThat(actual)
          .hasSize(1)
          .containsExactly(mockLog);

      verify(fetchStampLogPort).fetchDailyLogs(targetDate);
    }

    /**
     * 【テスト観点 / 責務】 境界値検証。出力ポートから打刻ログが存在しない（空リスト）旨が返却された場合、 エラーとならず安全に空リストが返却されることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>対象日付: {@code 2026-04-20}</li>
     *   <li>出力ポートが空リスト（{@code Collections.emptyList()}）を返却するようモック設定</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>返却されたリストが空であること</li>
     *   <li>出力ポートが呼び出されたこと</li>
     * </ul>
     */
    @Test
    @DisplayName("境界値: 対象日の打刻ログが0件の場合、空リストを返却すること")
    void shouldReturnEmptyListWhenPortReturnsEmpty() {
      // [Given]
      LocalDate targetDate = LocalDate.of(2026, 4, 20);
      when(fetchStampLogPort.fetchDailyLogs(targetDate)).thenReturn(Collections.emptyList());

      // [When]
      List<StampLog> actual = interactor.getDailyLogs(targetDate);

      // [Then]
      assertThat(actual).isEmpty();
      verify(fetchStampLogPort).fetchDailyLogs(targetDate);
    }

    /**
     * 【テスト観点 / 責務】 異常系検証。出力ポートで外部 API 通信障害（{@link ExternalIntegrationException}）が発生した場合、
     * ユースケース側で例外をもみ消さずにそのまま上位へ伝播させることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>対象日付: {@code 2026-04-20}</li>
     *   <li>出力ポートが {@link ExternalIntegrationException} をスローするよう設定</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>{@link ExternalIntegrationException} がスローされること</li>
     * </ul>
     */
    @Test
    @DisplayName("異常系: 出力ポートで外部連携例外が発生した場合、例外がそのまま伝播すること")
    void shouldPropagateExceptionWhenPortThrowsException() {
      // [Given]
      LocalDate targetDate = LocalDate.of(2026, 4, 20);
      when(fetchStampLogPort.fetchDailyLogs(targetDate))
          .thenThrow(new ExternalIntegrationException("HRMOS API 疎通エラー"));

      // [When & Then]
      assertThatThrownBy(() -> interactor.getDailyLogs(targetDate))
          .isInstanceOf(ExternalIntegrationException.class)
          .hasMessage("HRMOS API 疎通エラー");

      verify(fetchStampLogPort).fetchDailyLogs(targetDate);
    }
  }

  @Nested
  @DisplayName("特定ユーザーの期間打刻履歴取得（getUserLogs）シナリオ")
  class GetUserLogsScenario {

    private final String employeeNumber = "EMP001";
    private final LocalDate fromDate = LocalDate.of(2026, 4, 1);
    private final LocalDate toDate = LocalDate.of(2026, 4, 15);

    /**
     * 【テスト観点 / 責務】 正常系検証。社員番号、開始日、終了日の全パラメータが出力ポート
     * （{@link FetchStampLogPort#fetchUserLogs(String, LocalDate, LocalDate)}）へ正確に委譲され、
     * 取得結果がそのまま返却されることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>社員番号: {@code "EMP001"}、開始日: {@code 2026-04-01}、終了日: {@code 2026-04-15}</li>
     *   <li>出力ポートが 2 件の打刻ログを含むリストを返却するようモック設定</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>返却されたリストの要素数が 2 件であり、期待する打刻ログと一致すること</li>
     *   <li>出力ポートが指定されたパラメータで実行されたこと</li>
     * </ul>
     */
    @Test
    @DisplayName("正常系: 指定従業員の期間打刻ログが出力ポートから取得され、そのまま返却されること")
    void shouldReturnUserLogsSuccessfully() {
      // [Given]
      StampLog mockLog1 = mock(StampLog.class);
      StampLog mockLog2 = mock(StampLog.class);
      List<StampLog> expectedLogs = List.of(mockLog1, mockLog2);

      when(fetchStampLogPort.fetchUserLogs(employeeNumber, fromDate, toDate))
          .thenReturn(expectedLogs);

      // [When]
      List<StampLog> actual = interactor.getUserLogs(employeeNumber, fromDate, toDate);

      // [Then]
      assertThat(actual)
          .hasSize(2)
          .containsExactly(mockLog1, mockLog2);

      verify(fetchStampLogPort).fetchUserLogs(employeeNumber, fromDate, toDate);
    }

    /**
     * 【テスト観点 / 責務】 境界値検証。期間内の打刻ログが存在しない場合、空リストが返却されることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>出力ポートが空リスト（{@code Collections.emptyList()}）を返却するようモック設定</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>返却されたリストが空であること</li>
     * </ul>
     */
    @Test
    @DisplayName("境界値: 期間内の打刻ログが0件の場合、空リストを返却すること")
    void shouldReturnEmptyListWhenPortReturnsEmptyForUser() {
      // [Given]
      when(fetchStampLogPort.fetchUserLogs(employeeNumber, fromDate, toDate))
          .thenReturn(Collections.emptyList());

      // [When]
      List<StampLog> actual = interactor.getUserLogs(employeeNumber, fromDate, toDate);

      // [Then]
      assertThat(actual).isEmpty();
      verify(fetchStampLogPort).fetchUserLogs(employeeNumber, fromDate, toDate);
    }

    /**
     * 【テスト観点 / 責務】 境界値検証。開始日および終了日が null（全期間指定等）の場合であっても、 例外をスローせず透過的に出力ポートへ委譲されることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>開始日: {@code null}、終了日: {@code null}</li>
     *   <li>出力ポートが空リストを返却するようモック設定</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>出力ポートに null の日付が引き渡され、正常に結果が返却されること</li>
     * </ul>
     */
    @Test
    @DisplayName("境界値: 開始日・終了日が null の場合でも、出力ポートへ透過的に委譲されること")
    void shouldDelegateToPortEvenWhenDatesAreNull() {
      // [Given]
      when(fetchStampLogPort.fetchUserLogs(employeeNumber, null, null))
          .thenReturn(Collections.emptyList());

      // [When]
      List<StampLog> actual = interactor.getUserLogs(employeeNumber, null, null);

      // [Then]
      assertThat(actual).isEmpty();
      verify(fetchStampLogPort).fetchUserLogs(employeeNumber, null, null);
    }

    /**
     * 【テスト観点 / 責務】 異常系検証。特定ユーザーの打刻ログ取得中に出力ポートで例外が発生した場合、 例外がそのまま上位へ伝播することを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>出力ポートが {@link RuntimeException} をスローするよう設定</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>{@link RuntimeException} がスローされること</li>
     * </ul>
     */
    @Test
    @DisplayName("異常系: 出力ポートで例外が発生した場合、例外がそのまま伝播すること")
    void shouldPropagateExceptionWhenPortThrowsExceptionForUser() {
      // [Given]
      when(fetchStampLogPort.fetchUserLogs(employeeNumber, fromDate, toDate))
          .thenThrow(new RuntimeException("通信タイムアウト"));

      // [When & Then]
      assertThatThrownBy(() -> interactor.getUserLogs(employeeNumber, fromDate, toDate))
          .isInstanceOf(RuntimeException.class)
          .hasMessage("通信タイムアウト");

      verify(fetchStampLogPort).fetchUserLogs(employeeNumber, fromDate, toDate);
    }
  }
}
