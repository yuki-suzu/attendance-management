package com.computer_rescuer.attendance_management.adapter.out.persistence;

import static com.computer_rescuer.attendance_management.generated.jooq.Tables.M_EMPLOYEE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.computer_rescuer.attendance_management.domain.model.Employee;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record8;
import org.jooq.RecordMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link EmployeeByIdAdapter} の単体テストクラス。
 * <p>
 * 従業員情報取得ポート（{@link
 * com.computer_rescuer.attendance_management.application.port.out.FetchEmployeeByIdPort}）の実装として、
 * 以下の責務、境界条件、および条件分岐（C1 カバレッジ 100%）を検証します:
 * <ul>
 *   <li>ユーザーIDリストが null または空リストの場合の早期リターン（空マップ返却）</li>
 *   <li>指定されたユーザーIDリストに基づく 8 カラム選択クエリの実行と、{@link Employee} ドメインモデルへのマッピング検証</li>
 * </ul>
 * 8 カラム選択（ID〜EMPLOYMENT_ID）に対応する {@link Record8} モックを使用し、型安全な単体テストとして実行されます。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmployeeByIdAdapter 単体テスト")
class EmployeeByIdAdapterTest {

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private DSLContext dsl;

  @InjectMocks
  private EmployeeByIdAdapter adapter;

  @Nested
  @DisplayName("入力パラメータ検証および早期リターンシナリオ")
  class ValidationScenario {

    /**
     * 【テスト観点 / 責務】 ユーザーIDリストが null の場合に、DB検索を行わず即座に空マップを返却することを確認します（C1 分岐網羅）。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>{@code userIds} に null を渡す</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>戻り値が空マップであること</li>
     *   <li>{@link DSLContext} が一切呼び出されないこと</li>
     * </ul>
     */
    @Test
    @DisplayName("境界値: ユーザーIDリストが null の場合、空マップを返却すること")
    void shouldReturnEmptyMapWhenUserIdsIsNull() {
      // [When]
      Map<Integer, Employee> actual = adapter.fetchEmployeeMapByUserIds(null);

      // [Then]
      assertThat(actual).isEmpty();
      verifyNoInteractions(dsl);
    }

    /**
     * 【テスト観点 / 責務】 ユーザーIDリストが空（要素数 0）の場合に、DB検索を行わず即座に空マップを返却することを確認します（C1 分岐網羅）。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>{@code userIds} に空リスト {@code Collections.emptyList()} を渡す</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>戻り値が空マップであること</li>
     *   <li>{@link DSLContext} が一切呼び出されないこと</li>
     * </ul>
     */
    @Test
    @DisplayName("境界値: ユーザーIDリストが空の場合、空マップを返却すること")
    void shouldReturnEmptyMapWhenUserIdsIsEmpty() {
      // [When]
      Map<Integer, Employee> actual = adapter.fetchEmployeeMapByUserIds(Collections.emptyList());

      // [Then]
      assertThat(actual).isEmpty();
      verifyNoInteractions(dsl);
    }
  }

  @Nested
  @DisplayName("従業員情報マップ一括取得（fetchEmployeeMapByUserIds）シナリオ")
  class FetchEmployeeMapScenario {

    /**
     * 【テスト観点 / 責務】 正常系検証。指定されたユーザーIDリストに対して正しい 8 カラム選択クエリが発行され、 取得結果が正確に {@link Employee}
     * ドメインモデルへマッピングされて返却されることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>ユーザーIDリスト: {@code List.of(101)}</li>
     *   <li>8カラム選択に対応する {@link Record8} モックを用意し、各項目の取得値を設定</li>
     *   <li>{@code fetchMap} に渡された本番コードのラムダ式を実行して結果を構築するようスタブ設定</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>ユーザーID 101 をキーとする完全な {@link Employee} インスタンスを含むマップが返却されること</li>
     *   <li>{@link DSLContext#select} から始まるクエリが実行されたこと</li>
     * </ul>
     */
    @Test
    @DisplayName("正常系: ユーザーIDリストに一致する従業員情報を取得し、Mapとして返却すること")
    void shouldFetchEmployeeMapSuccessfully() {
      // [Given]
      List<Integer> userIds = List.of(101);

      // 8カラム選択に対応する Record8 型をモック化
      Record8<Integer, String, String, String, String, Integer, Integer, Integer> record = mock();
      when(record.get(M_EMPLOYEE.ID)).thenReturn(101);
      when(record.get(M_EMPLOYEE.EMPLOYEE_NUMBER)).thenReturn("EMP001");
      when(record.get(M_EMPLOYEE.LAST_NAME)).thenReturn("山田");
      when(record.get(M_EMPLOYEE.FIRST_NAME)).thenReturn("太郎");
      when(record.get(M_EMPLOYEE.EMAIL)).thenReturn("yamada@example.com");
      when(record.get(M_EMPLOYEE.DEPARTMENT_ID)).thenReturn(10);
      when(record.get(M_EMPLOYEE.DEFAULT_SEGMENT_ID)).thenReturn(1);
      when(record.get(M_EMPLOYEE.EMPLOYMENT_ID)).thenReturn(2);

      // 8カラムのジェネリクス境界を指定して fetchMap のオーバーロードを解決
      when(dsl.select(
              M_EMPLOYEE.ID,
              M_EMPLOYEE.EMPLOYEE_NUMBER,
              M_EMPLOYEE.LAST_NAME,
              M_EMPLOYEE.FIRST_NAME,
              M_EMPLOYEE.EMAIL,
              M_EMPLOYEE.DEPARTMENT_ID,
              M_EMPLOYEE.DEFAULT_SEGMENT_ID,
              M_EMPLOYEE.EMPLOYMENT_ID
          )
          .from(M_EMPLOYEE)
          .where(any(Condition.class))
          .fetchMap(eq(M_EMPLOYEE.ID),
              ArgumentMatchers.<RecordMapper<Record8<Integer, String, String, String, String, Integer, Integer, Integer>, Employee>>any()))
          .thenAnswer(invocation -> {
            RecordMapper<Record8<Integer, String, String, String, String, Integer, Integer, Integer>, Employee> mapper = invocation.getArgument(
                1);
            return Map.of(101, Objects.requireNonNull(mapper.map(record)));
          });

      clearInvocations(dsl);

      // [When]
      Map<Integer, Employee> actual = adapter.fetchEmployeeMapByUserIds(userIds);

      // [Then]
      assertThat(actual).hasSize(1);
      Employee emp = actual.get(101);
      assertThat(emp).isNotNull();
      assertThat(emp.id()).isEqualTo(101);
      assertThat(emp.employeeNumber()).isEqualTo("EMP001");
      assertThat(emp.lastName()).isEqualTo("山田");
      assertThat(emp.firstName()).isEqualTo("太郎");
      assertThat(emp.email()).isEqualTo("yamada@example.com");
      assertThat(emp.departmentId()).isEqualTo(10);
      assertThat(emp.defaultSegmentId()).isEqualTo(1);
      assertThat(emp.employmentId()).isEqualTo(2);

      verify(dsl).select(
          M_EMPLOYEE.ID,
          M_EMPLOYEE.EMPLOYEE_NUMBER,
          M_EMPLOYEE.LAST_NAME,
          M_EMPLOYEE.FIRST_NAME,
          M_EMPLOYEE.EMAIL,
          M_EMPLOYEE.DEPARTMENT_ID,
          M_EMPLOYEE.DEFAULT_SEGMENT_ID,
          M_EMPLOYEE.EMPLOYMENT_ID
      );
    }
  }
}
