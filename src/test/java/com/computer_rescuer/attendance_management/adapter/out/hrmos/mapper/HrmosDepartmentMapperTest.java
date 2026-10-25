package com.computer_rescuer.attendance_management.adapter.out.hrmos.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.computer_rescuer.attendance_management.adapter.out.hrmos.model.HrmosDepartment;
import com.computer_rescuer.attendance_management.domain.model.Department;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

/**
 * {@link HrmosDepartmentMapper} の単体テストクラス。
 * <p>
 * HRMOS 部門モデルからドメインモデル（{@link Department}）への相互変換において、 以下の責務および境界条件（C1 カバレッジ 100%）を検証します:
 * <ul>
 *   <li>全プロパティ（id, name, sequence）の完全マッピング</li>
 *   <li>単一オブジェクトおよびリスト変換における null 安全性・空コレクションの透過性</li>
 * </ul>
 * Spring コンテキストを起動せず、MapStruct のファクトリを用いて高速かつ決定論的に実行されます。
 * </p>
 */
@DisplayName("HrmosDepartmentMapper 単体テスト")
class HrmosDepartmentMapperTest {

  private HrmosDepartmentMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = Mappers.getMapper(HrmosDepartmentMapper.class);
  }

  @Nested
  @DisplayName("単一モデルマッピング（toDomain）シナリオ")
  class ToDomainScenario {

    /**
     * 【テスト観点 / 責務】 正常系検証。HRMOS 部門モデルの全フィールドが、ドメインモデルの対応するフィールドへ 正確にマッピングされることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>{@code id}=10、{@code name}="人事総務部"、{@code sequence}=1 の {@link HrmosDepartment} を用意</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>変換後の {@link Department} のプロパティが元の値と完全一致すること</li>
     * </ul>
     */
    @Test
    @DisplayName("正常系: HrmosDepartment の各フィールドが Department へ正確に変換されること")
    void shouldMapHrmosDepartmentToDepartmentSuccessfully() {
      // [Given]
      HrmosDepartment hrmosDepartment = new HrmosDepartment(10, "人事総務部", 1);

      // [When]
      Department actual = mapper.toDomain(hrmosDepartment);

      // [Then]
      assertThat(actual).isNotNull();
      assertThat(actual.id()).isEqualTo(10);
      assertThat(actual.name()).isEqualTo("人事総務部");
      assertThat(actual.sequence()).isEqualTo(1);
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
    void shouldReturnNullWhenHrmosDepartmentIsNull() {
      // [When]
      Department actual = mapper.toDomain(null);

      // [Then]
      assertThat(actual).isNull();
    }
  }

  @Nested
  @DisplayName("一括モデルマッピング（toDomainList）シナリオ")
  class ToDomainListScenario {

    /**
     * 【テスト観点 / 責務】 リスト変換の正常系検証。複数件の HRMOS 部門モデルを含むリストが、 順序と件数を保持したままドメインモデルのリストへ変換されることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>2 件の {@link HrmosDepartment} を含むリストを用意</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>返却されるリストのサイズが 2 件であること</li>
     *   <li>各要素の項目（部門ID、部門名等）が正確にマッピングされていること</li>
     * </ul>
     */
    @Test
    @DisplayName("正常系: HrmosDepartment のリストが Department のリストへ順序を維持して一括変換されること")
    void shouldMapHrmosDepartmentListToDepartmentListSuccessfully() {
      // [Given]
      HrmosDepartment dept1 = new HrmosDepartment(1, "経営企画部", 1);
      HrmosDepartment dept2 = new HrmosDepartment(2, "エンジニアリング部", 2);
      List<HrmosDepartment> deptList = List.of(dept1, dept2);

      // [When]
      List<Department> actualList = mapper.toDomainList(deptList);

      // [Then]
      assertThat(actualList)
          .isNotNull()
          .hasSize(2);

      assertThat(actualList.getFirst().id()).isEqualTo(1);
      assertThat(actualList.getFirst().name()).isEqualTo("経営企画部");
      assertThat(actualList.getFirst().sequence()).isEqualTo(1);

      assertThat(actualList.get(1).id()).isEqualTo(2);
      assertThat(actualList.get(1).name()).isEqualTo("エンジニアリング部");
      assertThat(actualList.get(1).sequence()).isEqualTo(2);
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
      List<Department> actual = mapper.toDomainList(Collections.emptyList());

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
      List<Department> actual = mapper.toDomainList(null);

      // [Then]
      assertThat(actual).isNull();
    }
  }
}
