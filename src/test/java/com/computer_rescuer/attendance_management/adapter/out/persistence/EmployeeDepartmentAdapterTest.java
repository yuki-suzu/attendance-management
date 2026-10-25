package com.computer_rescuer.attendance_management.adapter.out.persistence;

import static com.computer_rescuer.attendance_management.generated.jooq.Tables.M_DEPARTMENT;
import static com.computer_rescuer.attendance_management.generated.jooq.Tables.M_EMPLOYEE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record2;
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
 * {@link EmployeeDepartmentAdapter} の単体テストクラス。
 * <p>
 * 従業員マスタと部門マスタを結合して部門名マップを一括取得するデータアクセスアダプターとして、 以下の責務、境界条件、および条件分岐（C1 カバレッジ 100%）を検証します:
 * <ul>
 *   <li>入力値（ユーザーIDリスト）が null または空リストの場合の早期リターン（空マップ返却と DB 非アクセス）</li>
 *   <li>所属部門が存在する従業員において、正しい部門名がマッピングされる正常系パス</li>
 *   <li>LEFT JOIN の結果、部門が存在しない（DEPARTMENT_ID が null など）従業員に対する「未所属」フォールバック分岐</li>
 * </ul>
 * jOOQ の Fluent API に対しては {@link Answers#RETURNS_DEEP_STUBS} を適用し、
 * スタブ定義時の事前呼び出しカウントを {@link org.mockito.Mockito#clearInvocations(Object...)} でリセットした上で、
 * 内部の {@link RecordMapper} ラムダ式に {@link Record2} モックを渡して三項演算子の両ルートを完全に検証します。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmployeeDepartmentAdapter 単体テスト")
class EmployeeDepartmentAdapterTest {

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private DSLContext dsl;

  @InjectMocks
  private EmployeeDepartmentAdapter adapter;

  @Nested
  @DisplayName("入力パラメータ検証および早期リターンシナリオ")
  class ValidationScenario {

    /**
     * 【テスト観点 / 責務】 ユーザーIDリストが null の場合に、データベースへの問い合わせを行わずに
     * 即座に空のマップ（{@code Map.of()}）を返却することを確認します（C1 分岐網羅）。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>{@code userIds} に null を渡す</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>戻り値が空のマップであること</li>
     *   <li>{@link DSLContext} への問い合わせ処理が一切呼び出されないこと</li>
     * </ul>
     */
    @Test
    @DisplayName("境界値: ユーザーIDリストが null の場合、DB検索を行わず空マップを返却すること")
    void shouldReturnEmptyMapWhenUserIdsIsNull() {
      // [When]
      Map<Integer, String> actual = adapter.fetchDepartmentMapByUserIds(null);

      // [Then]
      assertThat(actual).isEmpty();
      verifyNoInteractions(dsl);
    }

    /**
     * 【テスト観点 / 責務】 ユーザーIDリストが空（要素数 0）の場合に、データベースへの問い合わせを行わずに
     * 即座に空のマップ（{@code Map.of()}）を返却することを確認します（C1 分岐網羅）。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>{@code userIds} に空リスト {@code Collections.emptyList()} を渡す</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>戻り値が空のマップであること</li>
     *   <li>{@link DSLContext} への問い合わせ処理が一切呼び出されないこと</li>
     * </ul>
     */
    @Test
    @DisplayName("境界値: ユーザーIDリストが空の場合、DB検索を行わず空マップを返却すること")
    void shouldReturnEmptyMapWhenUserIdsIsEmpty() {
      // [When]
      Map<Integer, String> actual = adapter.fetchDepartmentMapByUserIds(Collections.emptyList());

      // [Then]
      assertThat(actual).isEmpty();
      verifyNoInteractions(dsl);
    }
  }

  @Nested
  @DisplayName("部門名マップ一括取得（fetchDepartmentMapByUserIds）シナリオ")
  class FetchDepartmentMapScenario {

    /**
     * 【テスト観点 / 責務】 正常系および C1 分岐網羅検証。 複数従業員の問い合わせにおいて、部門が設定されている従業員にはその部門名が、
     * 所属部門が存在しない（null）従業員にはフォールバック文字列「未所属」がそれぞれ正しくマッピングされることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>ユーザーIDリスト: {@code List.of(101, 102)}</li>
     *   <li>2カラム選択（ID, NAME）に対応する {@link Record2} モックを用意</li>
     *   <li>jOOQ レコード1（ID: 101）: 部門名 {@code "システム開発部"}</li>
     *   <li>jOOQ レコード2（ID: 102）: 部門名 {@code null}（未所属）</li>
     *   <li>{@code fetchMap} に渡された本番コードの {@link RecordMapper} を実際に評価して結果を構築するようスタブ設定</li>
     *   <li>スタブ時の呼び出し履歴を {@code clearInvocations(dsl)} でリセットすること</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>取得されたマップの要素数が 2 件であること</li>
     *   <li>ユーザーID 101 に対する部門名が {@code "システム開発部"} であること</li>
     *   <li>ユーザーID 102 に対する部門名が {@code "未所属"}（フォールバック）であること</li>
     *   <li>{@link DSLContext#select} から始まるクエリが実行（1回）されたこと</li>
     * </ul>
     */
    @Test
    @DisplayName("正常系・C1網羅: 部門名が存在する場合は名称を、nullの場合は「未所属」を返却すること")
    void shouldFetchDepartmentMapWithFallbackToUnknownDepartment() {
      // [Given]
      List<Integer> userIds = List.of(101, 102);

      // 💡 2カラム選択に対応する Record2 型を指定してスタブを設定
      when(dsl.select(M_EMPLOYEE.ID, M_DEPARTMENT.NAME)
          .from(M_EMPLOYEE)
          .leftJoin(M_DEPARTMENT).on(any(Condition.class))
          .where(any(Condition.class))
          .fetchMap(eq(M_EMPLOYEE.ID),
              ArgumentMatchers.<RecordMapper<Record2<Integer, String>, String>>any()))
          .thenAnswer(invocation -> {
            RecordMapper<Record2<Integer, String>, String> mapper = invocation.getArgument(1);

            // 分岐A: 部門名が存在するレコード (Trueルート) - Record2 をモック化
            Record2<Integer, String> recordWithDept = mock(Record2.class);
            when(recordWithDept.get(M_DEPARTMENT.NAME)).thenReturn("システム開発部");

            // 分岐B: 部門名が null のレコード (Falseルート -> "未所属") - Record2 をモック化
            Record2<Integer, String> recordWithoutDept = mock(Record2.class);
            when(recordWithoutDept.get(M_DEPARTMENT.NAME)).thenReturn(null);

            return Map.of(
                101, mapper.map(recordWithDept),
                102, mapper.map(recordWithoutDept)
            );
          });

      // 💡 Deep Stub 定義時の呼び出し履歴をクリアし、本番コードの実行のみを検証対象にする
      clearInvocations(dsl);

      // [When]
      Map<Integer, String> actual = adapter.fetchDepartmentMapByUserIds(userIds);

      // [Then]
      assertThat(actual)
          .hasSize(2)
          .containsEntry(101, "システム開発部")
          .containsEntry(102, "未所属");

      verify(dsl).select(M_EMPLOYEE.ID, M_DEPARTMENT.NAME);
    }
  }
}
