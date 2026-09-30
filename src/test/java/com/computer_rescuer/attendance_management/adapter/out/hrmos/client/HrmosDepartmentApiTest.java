package com.computer_rescuer.attendance_management.adapter.out.hrmos.client;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.computer_rescuer.attendance_management.adapter.out.hrmos.model.HrmosDepartment;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.core.type.TypeReference;

/**
 * {@link HrmosDepartmentApi} の単体テストクラス。
 * <p>
 * HRMOS 部門 API ファサードとして、エンドポイントパス（"/departments"）および リソース名（"部門"）の型安全な引き渡し検証（C0 100%）を行います。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HrmosDepartmentApi 単体テスト")
class HrmosDepartmentApiTest {

  @Mock
  private HrmosCoreHttpClient coreClient;

  @InjectMocks
  private HrmosDepartmentApi departmentApi;

  @Nested
  @DisplayName("部門一覧取得（fetchDepartments）シナリオ")
  class FetchDepartmentsScenario {

    /**
     * 正しい引数でコアクライアントが呼び出され、取得された部門リストが返却されることを検証します。
     */
    @Test
    @DisplayName("正常系: 部門エンドポイントに対してコアクライアントを呼び出し、部門リストを返却すること")
    void shouldFetchDepartmentsThroughCoreClient() {
      // [Given]
      String token = "dummy-token";
      HrmosDepartment department = new HrmosDepartment(1, "開発部", 1);
      List<HrmosDepartment> expectedList = List.of(department);

      given(coreClient.fetchAndParseList(
          eq(token),
          eq("/departments"),
          eq(1),
          eq("部門"),
          any(TypeReference.class)
      )).willReturn(expectedList);

      // [When]
      List<HrmosDepartment> actualList = departmentApi.fetchDepartments(token);

      // [Then]
      assertAll(
          () -> assertNotNull(actualList, "返却リストが null でないこと"),
          () -> assertEquals(1, actualList.size(), "要素数が一致すること"),
          () -> assertEquals(expectedList, actualList, "取得結果が一致すること")
      );
      then(coreClient).should().fetchAndParseList(
          eq(token),
          eq("/departments"),
          eq(1),
          eq("部門"),
          any(TypeReference.class)
      );
    }
  }
}
