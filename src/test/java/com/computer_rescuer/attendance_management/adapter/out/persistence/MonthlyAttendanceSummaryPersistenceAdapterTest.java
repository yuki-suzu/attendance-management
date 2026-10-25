package com.computer_rescuer.attendance_management.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.computer_rescuer.attendance_management.adapter.out.persistence.jooq.MonthlyAttendanceSummaryJooqRepository;
import com.computer_rescuer.attendance_management.application.port.out.MonthlyAttendanceSummaryRepositoryPort.PersistedSummary;
import com.computer_rescuer.attendance_management.application.port.out.MonthlyAttendanceSummaryRepositoryPort.SummaryItem;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link MonthlyAttendanceSummaryPersistenceAdapter} の単体テストクラス。
 * <p>
 * 月次勤怠サマリ出力ポート（{@link
 * com.computer_rescuer.attendance_management.application.port.out.MonthlyAttendanceSummaryRepositoryPort}）の実装として、
 * リポジトリ（{@link MonthlyAttendanceSummaryJooqRepository}）への UPSERT、検索、送信完了更新、およびパージ処理の引数透過委譲を検証します。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MonthlyAttendanceSummaryPersistenceAdapter 単体テスト")
class MonthlyAttendanceSummaryPersistenceAdapterTest {

  @Mock
  private MonthlyAttendanceSummaryJooqRepository jooqRepository;

  @InjectMocks
  private MonthlyAttendanceSummaryPersistenceAdapter adapter;

  private final String procMonth = "2026-04";

  @Nested
  @DisplayName("UPSERT処理（upsertSummaries）シナリオ")
  class UpsertSummariesScenario {

    /**
     * 【テスト観点 / 責務】 サマリ項目の UPSERT 実行時、渡された引数がそのままリポジトリへ透過されることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>社員番号 {@code "EMP001"}、処理年月 {@code "2026-04"}、サマリ項目リスト</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>{@link MonthlyAttendanceSummaryJooqRepository#upsertSummaries(String, String, List)} が呼び出されること</li>
     * </ul>
     */
    @Test
    @DisplayName("正常系: upsertSummaries 実行時、リポジトリに同一引数が渡されること")
    void shouldDelegateUpsertSummariesToRepository() {
      // [Given]
      String empNumber = "EMP001";
      SummaryItem item = new SummaryItem(2, "予定休", 8, "1");
      List<SummaryItem> items = List.of(item);

      // [When]
      adapter.upsertSummaries(procMonth, empNumber, items);

      // [Then]
      verify(jooqRepository).upsertSummaries(procMonth, empNumber, items);
    }
  }

  @Nested
  @DisplayName("未送信従業員番号検索（findEmployeeNumbersWithUnsentSummary）シナリオ")
  class FindEmployeeNumbersWithUnsentSummaryScenario {

    /**
     * 【テスト観点 / 責務】 未送信従業員番号の検索時、リポジトリから取得されたリストがそのまま返却されることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>リポジトリが {@code List.of("EMP001", "EMP002")} を返却するよう設定</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>戻り値のリストがリポジトリの結果と一致すること</li>
     * </ul>
     */
    @Test
    @DisplayName("正常系: findEmployeeNumbersWithUnsentSummary 実行時、未送信従業員番号リストを返却すること")
    void shouldReturnUnsentEmployeeNumbersFromRepository() {
      // [Given]
      List<String> expectedList = List.of("EMP001", "EMP002");
      when(jooqRepository.selectEmployeeNumbersWithUnsent(procMonth)).thenReturn(expectedList);

      // [When]
      List<String> actual = adapter.findEmployeeNumbersWithUnsentSummary(procMonth);

      // [Then]
      assertThat(actual).isEqualTo(expectedList);
      verify(jooqRepository).selectEmployeeNumbersWithUnsent(procMonth);
    }
  }

  @Nested
  @DisplayName("サマリ全量検索（findSummariesByEmployeeNumbers）シナリオ")
  class FindSummariesByEmployeeNumbersScenario {

    /**
     * 【テスト観点 / 責務】 指定された従業員番号群のサマリ検索時、リポジトリから取得されたリストがそのまま返却されることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>従業員番号リスト {@code List.of("EMP001")}</li>
     *   <li>リポジトリが永続化サマリリストを返却するよう設定</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>戻り値のサマリリストがリポジトリの結果と一致すること</li>
     * </ul>
     */
    @Test
    @DisplayName("正常系: findSummariesByEmployeeNumbers 実行時、永続化サマリリストを返却すること")
    void shouldReturnPersistedSummariesFromRepository() {
      // [Given]
      List<String> empNumbers = List.of("EMP001");
      PersistedSummary summary = mock(PersistedSummary.class);
      List<PersistedSummary> expectedSummaries = List.of(summary);

      when(jooqRepository.selectSummariesByEmployeeNumbers(procMonth, empNumbers))
          .thenReturn(expectedSummaries);

      // [When]
      List<PersistedSummary> actual = adapter.findSummariesByEmployeeNumbers(procMonth, empNumbers);

      // [Then]
      assertThat(actual).isEqualTo(expectedSummaries);
      // 💡 正しいメソッド呼び出しを検証
      verify(jooqRepository).selectSummariesByEmployeeNumbers(procMonth, empNumbers);
    }
  }

  @Nested
  @DisplayName("送信完了更新（markAsSent）シナリオ")
  class MarkAsSentScenario {

    /**
     * 【テスト観点 / 責務】 送信完了フラグ更新時、リポジトリの更新メソッドに引数がそのまま渡されることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>従業員番号リスト {@code List.of("EMP001")}</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>{@link MonthlyAttendanceSummaryJooqRepository#updateMsgSendToSent(String, List)} が呼び出されること</li>
     * </ul>
     */
    @Test
    @DisplayName("正常系: markAsSent 実行時、リポジトリの更新メソッドが呼び出されること")
    void shouldDelegateUpdateMsgSendToSentToRepository() {
      // [Given]
      List<String> empNumbers = List.of("EMP001");

      // [When]
      adapter.markAsSent(procMonth, empNumbers);

      // [Then]
      verify(jooqRepository).updateMsgSendToSent(procMonth, empNumbers);
    }
  }

  @Nested
  @DisplayName("古いレコード削除（deleteOlderThan）シナリオ")
  class DeleteOlderThanScenario {

    /**
     * 【テスト観点 / 責務】 保持期間超過レコードの削除時、リポジトリが返却した削除件数がそのまま返却されることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>削除基準日時 {@code 2025-10-01T00:00:00}</li>
     *   <li>削除件数 {@code 10} 件</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>戻り値の削除件数が 10 であること</li>
     * </ul>
     */
    @Test
    @DisplayName("正常系: deleteOlderThan 実行時、リポジトリから返却された削除件数を返すこと")
    void shouldReturnDeletedCountFromRepository() {
      // [Given]
      LocalDateTime threshold = LocalDateTime.of(2025, 10, 1, 0, 0, 0);
      when(jooqRepository.deleteOlderThan(threshold)).thenReturn(10);

      // [When]
      int actual = adapter.deleteOlderThan(threshold);

      // [Then]
      assertThat(actual).isEqualTo(10);
      verify(jooqRepository).deleteOlderThan(threshold);
    }
  }
}
