package com.computer_rescuer.attendance_management.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.computer_rescuer.attendance_management.adapter.out.persistence.jooq.CheckedEmployeeJooqRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link CheckedEmployeePersistenceAdapter} の単体テストクラス。
 * <p>
 * チェック済み従業員情報出力ポート（{@link
 * com.computer_rescuer.attendance_management.application.port.out.CheckedEmployeeRepositoryPort}）の実装として、
 * リポジトリ（{@link CheckedEmployeeJooqRepository}）に対するチェック済みID検索、当月累積回数集計、
 * 一括重複抑止登録、および古いレコード削除の委譲を検証します。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CheckedEmployeePersistenceAdapter 単体テスト")
class CheckedEmployeePersistenceAdapterTest {

  @Mock
  private CheckedEmployeeJooqRepository jooqRepository;

  @InjectMocks
  private CheckedEmployeePersistenceAdapter adapter;

  private final LocalDate targetDate = LocalDate.of(2026, 4, 20);
  private final List<Integer> employeeIds = List.of(101, 102);

  @Nested
  @DisplayName("当日チェック済みID検索（findCheckedEmployeeIds）シナリオ")
  class FindCheckedEmployeeIdsScenario {

    /**
     * 【テスト観点 / 責務】 当日チェック済みIDの検索時、リポジトリから取得された従業員IDセットがそのまま返却されることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>リポジトリが {@code Set.of(101)} を返却するよう設定</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>戻り値のセットが {@code Set.of(101)} と一致すること</li>
     * </ul>
     */
    @Test
    @DisplayName("正常系: findCheckedEmployeeIds 実行時、リポジトリから返却されたセットを返すこと")
    void shouldReturnCheckedEmployeeIdsFromRepository() {
      // [Given]
      Set<Integer> expectedSet = Set.of(101);
      when(jooqRepository.selectCheckedEmployeeIds(targetDate, employeeIds)).thenReturn(
          expectedSet);

      // [When]
      Set<Integer> actual = adapter.findCheckedEmployeeIds(targetDate, employeeIds);

      // [Then]
      assertThat(actual).isEqualTo(expectedSet);
      verify(jooqRepository).selectCheckedEmployeeIds(targetDate, employeeIds);
    }
  }

  @Nested
  @DisplayName("当月累積回数集計（countMonthlyChecked）シナリオ")
  class CountMonthlyCheckedScenario {

    /**
     * 【テスト観点 / 責務】 当月累積回数の集計時、リポジトリから取得されたマップがそのまま返却されることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>リポジトリが {@code Map.of(101, 3)} を返却するよう設定</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>戻り値のマップが一致すること</li>
     * </ul>
     */
    @Test
    @DisplayName("正常系: countMonthlyChecked 実行時、リポジトリから返却されたカウントマップを返すこと")
    void shouldReturnMonthlyCountsFromRepository() {
      // [Given]
      Map<Integer, Integer> expectedMap = Map.of(101, 3);
      when(jooqRepository.countMonthlyChecked(targetDate, employeeIds)).thenReturn(expectedMap);

      // [When]
      Map<Integer, Integer> actual = adapter.countMonthlyChecked(targetDate, employeeIds);

      // [Then]
      assertThat(actual).isEqualTo(expectedMap);
      verify(jooqRepository).countMonthlyChecked(targetDate, employeeIds);
    }
  }

  @Nested
  @DisplayName("一括登録（saveAll）シナリオ")
  class SaveAllScenario {

    /**
     * 【テスト観点 / 責務】 一括登録要求を受け取った際、リポジトリの重複無視挿入（batchInsertIgnoreDuplicates）へ引数が透過されることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>事由コード {@code "01"}</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>{@link CheckedEmployeeJooqRepository#batchInsertIgnoreDuplicates(LocalDate, List, String)} が呼び出されること</li>
     * </ul>
     */
    @Test
    @DisplayName("正常系: saveAll 実行時、リポジトリの batchInsertIgnoreDuplicates が呼び出されること")
    void shouldDelegateBatchInsertIgnoreDuplicatesToRepository() {
      // [Given]
      String reasonCode = "01";

      // [When]
      adapter.saveAll(targetDate, employeeIds, reasonCode);

      // [Then]
      verify(jooqRepository).batchInsertIgnoreDuplicates(targetDate, employeeIds, reasonCode);
    }
  }

  @Nested
  @DisplayName("古い履歴削除（deleteOlderThan）シナリオ")
  class DeleteOlderThanScenario {

    /**
     * 【テスト観点 / 責務】 保持期間超過レコードの削除時、リポジトリが返却した削除件数がそのまま返却されることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>削除基準日 {@code 2025-04-20}</li>
     *   <li>削除件数 {@code 15} 件</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>戻り値の削除件数が 15 であること</li>
     * </ul>
     */
    @Test
    @DisplayName("正常系: deleteOlderThan 実行時、リポジトリから返却された削除件数を返すこと")
    void shouldReturnDeletedCountFromRepository() {
      // [Given]
      LocalDate threshold = LocalDate.of(2025, 4, 20);
      when(jooqRepository.deleteOlderThan(threshold)).thenReturn(15);

      // [When]
      int actual = adapter.deleteOlderThan(threshold);

      // [Then]
      assertThat(actual).isEqualTo(15);
      verify(jooqRepository).deleteOlderThan(threshold);
    }
  }
}
