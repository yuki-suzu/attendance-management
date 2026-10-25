package com.computer_rescuer.attendance_management.adapter.out.persistence;

import static com.computer_rescuer.attendance_management.generated.jooq.Tables.M_EMPLOYEE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link EmployeeIdMappingAdapter} の単体テストクラス。
 * <p>
 * 社員番号から HRMOS の内部ユーザーID（{@code user_id}）を解決するデータアクセスアダプターとして、 以下の境界条件および条件分岐（C1 カバレッジ
 * 100%）を検証します:
 * <ul>
 *   <li>入力値（社員番号）が null、空文字、または空白文字の場合の早期リターン（{@link Optional#empty()}）</li>
 *   <li>データベース上に該当社員が存在し、正常に {@code user_id}（Integer）が解決される正常系パス</li>
 *   <li>データベース上に該当社員が存在しない場合（クエリ結果が null）の未ヒット分岐および {@link Optional#empty()} 返却</li>
 * </ul>
 * jOOQ の Fluent API に対しては Mockito の {@link Answers#RETURNS_DEEP_STUBS} を適用し、
 * スタブ定義時の事前呼び出しカウントを {@link org.mockito.Mockito#clearInvocations(Object...)} でリセットすることで、
 * 実処理の呼び出し回数（1回）を厳密かつ決定論的に検証します。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmployeeIdMappingAdapter 単体テスト")
class EmployeeIdMappingAdapterTest {

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private DSLContext dsl;

  @InjectMocks
  private EmployeeIdMappingAdapter adapter;

  @Nested
  @DisplayName("入力パラメータ検証および早期リターンシナリオ")
  class ValidationScenario {

    /**
     * 【テスト観点 / 責務】 社員番号が null、空文字、または空白のみで構成されている場合に、 データベースへの問い合わせを行わずに即座に
     * {@link Optional#empty()} を返却することを確認します（C1 分岐網羅）。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>{@code employeeNumber} に null、空文字 {@code ""}、または空白文字列（半角スペース、タブ、改行）を渡す</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>戻り値が {@link Optional#empty()} であること</li>
     *   <li>{@link DSLContext} への問い合わせ処理が一切呼び出されないこと</li>
     * </ul>
     *
     * @param invalidEmployeeNumber テスト対象の不正な社員番号
     */
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("境界値: 社員番号が null または空白の場合、DB検索を行わず Optional.empty を返却すること")
    void shouldReturnEmptyOptionalWhenEmployeeNumberIsBlank(String invalidEmployeeNumber) {
      // [When]
      Optional<Integer> actual = adapter.resolve(invalidEmployeeNumber);

      // [Then]
      assertThat(actual).isEmpty();
      verifyNoInteractions(dsl);
    }
  }

  @Nested
  @DisplayName("社員番号解決（resolve）シナリオ")
  class ResolveScenario {

    /**
     * 【テスト観点 / 責務】 正常系検証。指定された社員番号に紐づくレコードが従業員マスタ（{@code m_employee}）に存在する場合、 jOOQ
     * クエリが正しくキックされ、該当する HRMOS ユーザーIDが {@link Optional} にラップされて返却されることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>社員番号 {@code "EMP001"}</li>
     *   <li>jOOQ クエリの末尾 {@code fetchOne(M_EMPLOYEE.ID)} の結果としてユーザーID {@code 101} が返却されるよう設定</li>
     *   <li>スタブ時の呼び出し履歴を {@code clearInvocations(dsl)} でリセットすること</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>戻り値が {@code Optional.of(101)} であること</li>
     *   <li>{@link DSLContext#select} から始まるクエリが実行（1回）されたこと</li>
     * </ul>
     */
    @Test
    @DisplayName("正常系: DBに対象社員が存在する場合、解決された HRMOS user_id を返却すること")
    void shouldResolveHrmosUserIdSuccessfully() {
      // [Given]
      String employeeNumber = "EMP001";
      Integer expectedUserId = 101;

      when(dsl.select(M_EMPLOYEE.ID)
          .from(M_EMPLOYEE)
          .where(any(Condition.class))
          .fetchOne(M_EMPLOYEE.ID))
          .thenReturn(expectedUserId);

      // 💡 Deep Stub 定義時の呼び出し履歴をクリアし、本番コードの呼び出しのみを検証対象にする
      clearInvocations(dsl);

      // [When]
      Optional<Integer> actual = adapter.resolve(employeeNumber);

      // [Then]
      assertThat(actual)
          .isPresent()
          .contains(expectedUserId);

      verify(dsl).select(M_EMPLOYEE.ID);
    }

    /**
     * 【テスト観点 / 責務】 境界値・C1 分岐検証。指定された社員番号がローカルデータベースに存在しない場合（クエリ結果が {@code null}）、
     * 未存在の警告ログ分岐を通過し、安全に {@link Optional#empty()} が返却されることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>社員番号 {@code "NOT_FOUND_EMP"}</li>
     *   <li>jOOQ クエリの末尾 {@code fetchOne(M_EMPLOYEE.ID)} の結果として {@code null} が返却されるよう設定</li>
     *   <li>スタブ時の呼び出し履歴を {@code clearInvocations(dsl)} でリセットすること</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>戻り値が {@link Optional#empty()} であること</li>
     *   <li>例外が発生せず正常にハンドリングされること</li>
     *   <li>{@link DSLContext#select} クエリが実行（1回）されたこと</li>
     * </ul>
     */
    @Test
    @DisplayName("境界値: DBに対象社員が存在しない場合、Optional.empty を返却すること")
    void shouldReturnEmptyOptionalWhenEmployeeNotFoundInDb() {
      // [Given]
      String employeeNumber = "NOT_FOUND_EMP";

      when(dsl.select(M_EMPLOYEE.ID)
          .from(M_EMPLOYEE)
          .where(any(Condition.class))
          .fetchOne(M_EMPLOYEE.ID))
          .thenReturn(null);

      // 💡 Deep Stub 定義時の呼び出し履歴をクリア
      clearInvocations(dsl);

      // [When]
      Optional<Integer> actual = adapter.resolve(employeeNumber);

      // [Then]
      assertThat(actual).isEmpty();

      verify(dsl).select(M_EMPLOYEE.ID);
    }
  }
}
