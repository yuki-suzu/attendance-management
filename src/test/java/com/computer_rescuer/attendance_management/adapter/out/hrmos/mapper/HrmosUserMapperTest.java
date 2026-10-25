package com.computer_rescuer.attendance_management.adapter.out.hrmos.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.computer_rescuer.attendance_management.adapter.out.hrmos.model.HrmosUser;
import com.computer_rescuer.attendance_management.domain.model.Employee;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

/**
 * {@link HrmosUserMapper} の単体テストクラス。
 * <p>
 * HRMOS ユーザーモデルから従業員ドメインモデル（{@link Employee}）への変換において、 以下の責務、境界条件、およびカスタムマッピング（C1 カバレッジ
 * 100%）を検証します:
 * <ul>
 *   <li>{@code number} から {@code employeeNumber} へのカスタムフィールド名マッピング</li>
 *   <li>同一名称・同一型の全プロパティ（id, names, email, IDs）の完全マッピング</li>
 *   <li>単一オブジェクトおよびリスト変換における null 安全性・空コレクションの透過性</li>
 * </ul>
 * Spring コンテキストを起動せず、MapStruct のファクトリを用いて高速かつ決定論的に実行されます。
 * </p>
 */
@DisplayName("HrmosUserMapper 単体テスト")
class HrmosUserMapperTest {

  private HrmosUserMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = Mappers.getMapper(HrmosUserMapper.class);
  }

  @Nested
  @DisplayName("単一モデルマッピング（toDomain）シナリオ")
  class ToDomainScenario {

    /**
     * 【テスト観点 / 責務】 正常系検証。HRMOS ユーザーモデルの全フィールドが、ドメインモデルの対応するフィールドへ
     * 正確にマッピングされることを確認します。特に「number」から「employeeNumber」への カスタムマッピングの正確性を検証します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>全項目が設定された {@link HrmosUser} インスタンスを用意</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>変換後の {@link Employee} の全プロパティが元の値と完全一致すること</li>
     *   <li>{@code employeeNumber()} に {@code hrmosUser.number()} の値が格納されていること</li>
     * </ul>
     */
    @Test
    @DisplayName("正常系: HrmosUser の各フィールド（特に number -> employeeNumber）が Employee へ正確に変換されること")
    void shouldMapHrmosUserToEmployeeSuccessfully() {
      // [Given]
      HrmosUser hrmosUser = new HrmosUser(
          101,
          "EMP-00123",
          "徳川",
          "家康",
          "ieyasu.tokugawa@example.com",
          5,
          2,
          1
      );

      // [When]
      Employee actual = mapper.toDomain(hrmosUser);

      // [Then]
      assertThat(actual).isNotNull();
      assertThat(actual.id()).isEqualTo(101);
      assertThat(actual.employeeNumber()).isEqualTo("EMP-00123");
      assertThat(actual.lastName()).isEqualTo("徳川");
      assertThat(actual.firstName()).isEqualTo("家康");
      assertThat(actual.email()).isEqualTo("ieyasu.tokugawa@example.com");
      assertThat(actual.departmentId()).isEqualTo(5);
      assertThat(actual.defaultSegmentId()).isEqualTo(2);
      assertThat(actual.employmentId()).isEqualTo(1);
    }

    /**
     * 【テスト観点 / 責務】 null 安全性の検証。入力が null の場合に例外を発生させず、安全に null を返却することを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>入力値として {@code null} を渡す</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>戻り値が {@code null} であること</li>
     * </ul>
     */
    @Test
    @DisplayName("null安全性: 入力が null の場合、null を返却すること")
    void shouldReturnNullWhenHrmosUserIsNull() {
      // [When]
      Employee actual = mapper.toDomain(null);

      // [Then]
      assertThat(actual).isNull();
    }
  }

  @Nested
  @DisplayName("一括モデルマッピング（toDomainList）シナリオ")
  class ToDomainListScenario {

    /**
     * 【テスト観点 / 責務】 リスト変換の正常系検証。複数件の HRMOS ユーザーモデルを含むリストが、 順序と件数を保持したままドメインモデルのリストへ変換されることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>2 件の {@link HrmosUser} を含むリストを用意</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>返却されるリストのサイズが 2 件であること</li>
     *   <li>各要素の項目（社員番号等）が正しくマッピングされていること</li>
     * </ul>
     */
    @Test
    @DisplayName("正常系: HrmosUser のリストが Employee のリストへ順序を維持して一括変換されること")
    void shouldMapHrmosUserListToEmployeeListSuccessfully() {
      // [Given]
      HrmosUser user1 = new HrmosUser(1, "A001", "織田", "信長", "nobu@ex.com", 1, 1, 1);
      HrmosUser user2 = new HrmosUser(2, "A002", "豊臣", "秀吉", "hide@ex.com", 2, 1, 1);
      List<HrmosUser> userList = List.of(user1, user2);

      // [When]
      List<Employee> actualList = mapper.toDomainList(userList);

      // [Then]
      assertThat(actualList)
          .isNotNull()
          .hasSize(2);

      assertThat(actualList.getFirst().id()).isEqualTo(1);
      assertThat(actualList.getFirst().employeeNumber()).isEqualTo("A001");
      assertThat(actualList.getFirst().lastName()).isEqualTo("織田");

      assertThat(actualList.get(1).id()).isEqualTo(2);
      assertThat(actualList.get(1).employeeNumber()).isEqualTo("A002");
      assertThat(actualList.get(1).lastName()).isEqualTo("豊臣");
    }

    /**
     * 【テスト観点 / 責務】 空リストの境界値検証。要素数 0 のリストが渡された場合に、空のリストが返却されることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>{@code Collections.emptyList()} を渡す</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>戻り値が空のリストであること</li>
     * </ul>
     */
    @Test
    @DisplayName("境界値: 空リストが渡された場合、空リストを返却すること")
    void shouldReturnEmptyListWhenInputListIsEmpty() {
      // [When]
      List<Employee> actual = mapper.toDomainList(Collections.emptyList());

      // [Then]
      assertThat(actual).isNotNull().isEmpty();
    }

    /**
     * 【テスト観点 / 責務】 null リストの境界値検証。入力リスト自体が null の場合に、安全に null が返却されることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>入力値として {@code null} を渡す</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>戻り値が {@code null} であること</li>
     * </ul>
     */
    @Test
    @DisplayName("null安全性: 入力リストが null の場合、null を返却すること")
    void shouldReturnNullWhenInputListIsNull() {
      // [When]
      List<Employee> actual = mapper.toDomainList(null);

      // [Then]
      assertThat(actual).isNull();
    }
  }
}
