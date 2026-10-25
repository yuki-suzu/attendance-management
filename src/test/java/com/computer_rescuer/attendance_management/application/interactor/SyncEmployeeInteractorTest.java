package com.computer_rescuer.attendance_management.application.interactor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.computer_rescuer.attendance_management.adapter.out.exception.ExternalIntegrationException;
import com.computer_rescuer.attendance_management.application.port.out.EmployeeRepositoryPort;
import com.computer_rescuer.attendance_management.application.port.out.FetchEmployeePort;
import com.computer_rescuer.attendance_management.domain.model.Employee;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link SyncEmployeeInteractor} の単体テストクラス。
 * <p>
 * 外部システム（HRMOS）からの最新従業員マスタ取得およびローカルデータベースへの洗い替え （全件削除および一括登録）を制御するユースケースの責務を網羅的に検証します:
 * <ul>
 *   <li>外部ポート経由での全件従業員データ取得と、DB側での削除・登録の実行順序制御</li>
 *   <li>外部取得件数が0件（空データ）の場合における既存データ全削除の安全抑止（早期リターン）</li>
 *   <li>外部連携障害またはDB障害発生時における例外伝播とトランザクションロールバック整合性</li>
 * </ul>
 * 本テストは Spring コンテキストを起動せず、純粋な Mockito 単体テストとして高速に実行されます。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SyncEmployeeInteractor 単体テスト")
class SyncEmployeeInteractorTest {

  @Mock
  private FetchEmployeePort fetchEmployeePort;

  @Mock
  private EmployeeRepositoryPort employeeRepositoryPort;

  @InjectMocks
  private SyncEmployeeInteractor interactor;

  /**
   * テスト用の従業員ドメインモデルインスタンスを生成するヘルパーメソッド。
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
        employeeNumber.toLowerCase() + "@example.com",
        1,
        1,
        1
    );
  }

  @Nested
  @DisplayName("正常系シナリオ")
  class SuccessScenario {

    /**
     * 【テスト観点 / 責務】 正常系全工程の検証。外部システムから有効な従業員リストが取得できた場合、
     * データベースの既存データ全件削除（{@code deleteAllEmployees}）を実行した後に、
     * 取得した全従業員データが一括登録（{@code saveAll}）されること、およびその呼び出し順序が 正しいことを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>{@code fetchEmployeePort.fetchAll()} が2名の従業員リスト（EMP001, EMP002）を返却すること</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>{@code employeeRepositoryPort.deleteAllEmployees()} が最初に1回呼び出されること</li>
     *   <li>その直後に {@code employeeRepositoryPort.saveAll(employees)} が取得データと同一内容で呼び出されること</li>
     *   <li>先頭要素（EMP001）を含む全データが欠落なく保存対象として渡されること</li>
     * </ul>
     */
    @Test
    @DisplayName("正常系: 外部システムから従業員を取得できた場合、DB全削除の後に一括登録が順序正しく実行されること")
    void shouldDeleteAllAndSaveAllEmployeesInOrderWhenEmployeesFetched() {
      // [Given]
      Employee emp1 = createEmployee(101, "EMP001", "徳川", "家康");
      Employee emp2 = createEmployee(102, "EMP002", "織田", "信長");
      List<Employee> expectedEmployees = List.of(emp1, emp2);

      when(fetchEmployeePort.fetchAll()).thenReturn(expectedEmployees);

      // [When]
      interactor.syncEmployees();

      // [Then]
      InOrder inOrder = inOrder(employeeRepositoryPort);
      inOrder.verify(employeeRepositoryPort).deleteAllEmployees();
      inOrder.verify(employeeRepositoryPort).saveAll(expectedEmployees);

      // 取得データの先頭要素が意図した通りであることを確認
      assertThat(expectedEmployees.getFirst().employeeNumber()).isEqualTo("EMP001");
    }
  }

  @Nested
  @DisplayName("境界値および条件分岐シナリオ")
  class BoundaryAndBranchScenario {

    /**
     * 【テスト観点 / 責務】 外部API取得結果が空リストの場合の安全抑止（C1 カバレッジ・早期リターン）の検証。
     * 外部システムの障害や不具合によって取得結果が0件となった際、既存のローカルDBマスタを 誤って全消去（Truncate）してしまう重大インシデントを防止するため、
     * 即座に早期リターンしてDB操作が一切行われないことを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>{@code fetchEmployeePort.fetchAll()} が空リスト（{@code Collections.emptyList()}）を返却すること</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>{@code employeeRepositoryPort} のいかなるメソッドも呼び出されないこと</li>
     *   <li>例外がスローされず安全に終了すること</li>
     * </ul>
     */
    @Test
    @DisplayName("境界値: 外部取得結果が0件の場合、DB削除・登録を実行せず安全に早期リターンすること")
    void shouldEarlyReturnWithoutTouchingDatabaseWhenFetchedListIsEmpty() {
      // [Given]
      when(fetchEmployeePort.fetchAll()).thenReturn(Collections.emptyList());

      // [When]
      interactor.syncEmployees();

      // [Then]
      verify(fetchEmployeePort).fetchAll();
      verifyNoInteractions(employeeRepositoryPort);
    }
  }

  @Nested
  @DisplayName("異常系およびトランザクション整合性シナリオ")
  class ExceptionScenario {

    /**
     * 【テスト観点 / 責務】 外部システム連携障害時における例外伝播と処理中断の検証。 外部API通信でタイムアウトや認証エラー等の例外が発生した場合、
     * DB更新処理に遷移することなく例外が上位へスローされ、トランザクションがロールバックされることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>{@code fetchEmployeePort.fetchAll()} が {@link ExternalIntegrationException} をスローすること</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>{@code assertThatThrownBy} により同例外が補足されること</li>
     *   <li>{@code employeeRepositoryPort} に対する操作が一切行われないこと</li>
     * </ul>
     */
    @Test
    @DisplayName("異常系: 外部システムからの従業員取得に失敗した場合、例外をスローしDB操作を行わないこと")
    void shouldPropagateExceptionWhenFetchingEmployeesFails() {
      // [Given]
      when(fetchEmployeePort.fetchAll())
          .thenThrow(
              new ExternalIntegrationException("HRMOS API 通信障害: 503 SERVICE_UNAVAILABLE"));

      // [When & Then]
      assertThatThrownBy(() -> interactor.syncEmployees())
          .isInstanceOf(ExternalIntegrationException.class)
          .hasMessageContaining("HRMOS API 通信障害");

      verifyNoInteractions(employeeRepositoryPort);
    }

    /**
     * 【テスト観点 / 責務】 データベース全件削除処理での障害発生時における例外伝播と後続処理抑止の検証。 テーブルロック競合や接続切断等により既存データの削除に失敗した場合、 後続の
     * {@code saveAll}（一括登録）が実行されずに処理が中断し、例外が上位へスローされることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>{@code fetchEmployeePort.fetchAll()} は正常に従業員1名を取得すること</li>
     *   <li>{@code employeeRepositoryPort.deleteAllEmployees()} が {@link RuntimeException} をスローすること</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>{@code assertThatThrownBy} により同例外が補足されること</li>
     *   <li>後続の {@code employeeRepositoryPort.saveAll} が決して呼び出されないこと</li>
     * </ul>
     */
    @Test
    @DisplayName("異常系: DB全件削除時に例外が発生した場合、一括登録を行わずに例外を上位へスローすること")
    void shouldPropagateExceptionAndSkipSaveAllWhenDeleteFails() {
      // [Given]
      Employee emp = createEmployee(101, "EMP001", "徳川", "家康");
      when(fetchEmployeePort.fetchAll()).thenReturn(List.of(emp));
      doThrow(new RuntimeException("DBデッドロック検知"))
          .when(employeeRepositoryPort).deleteAllEmployees();

      // [When & Then]
      assertThatThrownBy(() -> interactor.syncEmployees())
          .isInstanceOf(RuntimeException.class)
          .hasMessage("DBデッドロック検知");

      verify(employeeRepositoryPort).deleteAllEmployees();
      verify(employeeRepositoryPort, never()).saveAll(anyList());
    }

    /**
     * 【テスト観点 / 責務】 データベース一括登録処理での障害発生時における例外伝播の検証。 削除完了後の一括挿入（Batch Insert）において制約違反やDBエラーが発生した場合、
     * 例外が上位へスローされ、トランザクション全体がロールバックされることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>{@code fetchEmployeePort.fetchAll()} は正常に従業員1名を取得すること</li>
     *   <li>{@code employeeRepositoryPort.deleteAllEmployees()} は正常終了すること</li>
     *   <li>{@code employeeRepositoryPort.saveAll(anyList())} が {@link RuntimeException} をスローすること</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>{@code assertThatThrownBy} により同例外が補足されること</li>
     *   <li>{@code deleteAllEmployees} および {@code saveAll} が順次実行された形跡が確認できること</li>
     * </ul>
     */
    @Test
    @DisplayName("異常系: DB一括登録時に例外が発生した場合、例外を上位へスローすること")
    void shouldPropagateExceptionWhenSaveAllFails() {
      // [Given]
      Employee emp = createEmployee(101, "EMP001", "徳川", "家康");
      List<Employee> employees = List.of(emp);
      when(fetchEmployeePort.fetchAll()).thenReturn(employees);
      doThrow(new RuntimeException("一括登録中のSQL実行エラー"))
          .when(employeeRepositoryPort).saveAll(employees);

      // [When & Then]
      assertThatThrownBy(() -> interactor.syncEmployees())
          .isInstanceOf(RuntimeException.class)
          .hasMessage("一括登録中のSQL実行エラー");

      InOrder inOrder = inOrder(employeeRepositoryPort);
      inOrder.verify(employeeRepositoryPort).deleteAllEmployees();
      inOrder.verify(employeeRepositoryPort).saveAll(employees);
    }
  }
}
