package com.computer_rescuer.attendance_management.adapter.out.persistence;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.computer_rescuer.attendance_management.adapter.out.persistence.jooq.EmployeeJooqRepository;
import com.computer_rescuer.attendance_management.adapter.out.persistence.mapper.EmployeeJooqMapper;
import com.computer_rescuer.attendance_management.domain.model.Employee;
import com.computer_rescuer.attendance_management.generated.jooq.tables.records.MEmployeeRecord;
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
 * {@link EmployeePersistenceAdapter} の単体テストクラス。
 * <p>
 * 従業員マスタ永続化ポート（{@link
 * com.computer_rescuer.attendance_management.application.port.out.EmployeeRepositoryPort}）の実装として、
 * マッパー（{@link EmployeeJooqMapper}）および jOOQ リポジトリ（{@link EmployeeJooqRepository}）への
 * 正確な処理委譲とデータ変換連携を検証します。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmployeePersistenceAdapter 単体テスト")
class EmployeePersistenceAdapterTest {

  @Mock
  private EmployeeJooqRepository jooqRepository;

  @Mock
  private EmployeeJooqMapper mapper;

  @InjectMocks
  private EmployeePersistenceAdapter adapter;

  @Nested
  @DisplayName("全従業員削除（deleteAllEmployees）シナリオ")
  class DeleteAllEmployeesScenario {

    /**
     * 【テスト観点 / 責務】 全従業員削除の要求を受け取った際、リポジトリの全削除メソッドが呼び出されることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>特になし</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>{@link EmployeeJooqRepository#deleteAll()} が 1 回呼び出されること</li>
     * </ul>
     */
    @Test
    @DisplayName("正常系: deleteAllEmployees 実行時、リポジトリの deleteAll が呼び出されること")
    void shouldDelegateDeleteAllToRepository() {
      // [When]
      adapter.deleteAllEmployees();

      // [Then]
      verify(jooqRepository).deleteAll();
    }
  }

  @Nested
  @DisplayName("従業員一括登録（saveAll）シナリオ")
  class SaveAllScenario {

    /**
     * 【テスト観点 / 責務】 従業員ドメインモデルリストを登録する際、マッパーによって jOOQ レコードへ変換され、
     * リポジトリの一括挿入（batchInsert）へ渡されることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>従業員ドメインモデルリスト（要素数 1）</li>
     *   <li>マッパーの変換結果としてモックの {@link MEmployeeRecord} リストを用意</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>{@link EmployeeJooqMapper#toRecords(List)} が呼び出されること</li>
     *   <li>変換後のレコードリストが {@link EmployeeJooqRepository#batchInsert(List)} に渡されること</li>
     * </ul>
     */
    @Test
    @DisplayName("正常系: saveAll 実行時、マッパーで変換されたレコードが batchInsert に引き渡されること")
    void shouldMapAndDelegateBatchInsertToRepository() {
      // [Given]
      Employee employee = mock(Employee.class);
      List<Employee> employees = List.of(employee);

      MEmployeeRecord record = mock(MEmployeeRecord.class);
      List<MEmployeeRecord> records = List.of(record);

      when(mapper.toRecords(employees)).thenReturn(records);

      // [When]
      adapter.saveAll(employees);

      // [Then]
      verify(mapper).toRecords(employees);
      verify(jooqRepository).batchInsert(records);
    }

    /**
     * 【テスト観点 / 責務】 空の従業員リストが渡された場合でも例外をスローせず安全にマッピングと登録委譲が行われることを確認します（境界値）。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>空の従業員リスト {@code Collections.emptyList()}</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>空リストのままマッパーおよびリポジトリが呼び出されること</li>
     * </ul>
     */
    @Test
    @DisplayName("境界値: 空の従業員リストの場合、空のレコードリストで batchInsert が呼び出されること")
    void shouldHandleEmptyListGracefully() {
      // [Given]
      List<Employee> emptyEmployees = Collections.emptyList();
      List<MEmployeeRecord> emptyRecords = Collections.emptyList();

      when(mapper.toRecords(emptyEmployees)).thenReturn(emptyRecords);

      // [When]
      adapter.saveAll(emptyEmployees);

      // [Then]
      verify(mapper).toRecords(emptyEmployees);
      verify(jooqRepository).batchInsert(emptyRecords);
    }
  }
}
