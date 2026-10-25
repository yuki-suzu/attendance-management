package com.computer_rescuer.attendance_management.adapter.out.hrmos;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.computer_rescuer.attendance_management.adapter.out.hrmos.client.HrmosAuthApi;
import com.computer_rescuer.attendance_management.adapter.out.hrmos.client.HrmosUserApi;
import com.computer_rescuer.attendance_management.adapter.out.hrmos.mapper.HrmosUserMapper;
import com.computer_rescuer.attendance_management.adapter.out.hrmos.model.HrmosUser;
import com.computer_rescuer.attendance_management.domain.model.Employee;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link HrmosEmployeeAdapter} の単体テストクラス。
 * <p>
 * 外部システム（HRMOS）からの従業員情報取得出力ポートの実装として、 認証トークン取得、全件ページネーション取得、および従業員ドメインモデルへの マッピング連携における命令網羅（C0
 * 100%）を検証します。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HrmosEmployeeAdapter 単体テスト")
class HrmosEmployeeAdapterTest {

  @Mock
  private HrmosAuthApi authApi;

  @Mock
  private HrmosUserApi userApi;

  @Mock
  private HrmosUserMapper mapper;

  @InjectMocks
  private HrmosEmployeeAdapter adapter;

  private final String token = "test-auth-token";

  @Nested
  @DisplayName("全従業員情報取得（fetchAll）シナリオ")
  class FetchAllScenario {

    /**
     * 外部 API から従業員一覧が取得され、ドメインモデルへ正常に変換されて返却されることを検証します。
     */
    @Test
    @DisplayName("正常系: 全従業員情報をページネーション取得し、ドメインモデルのリストとして返却すること")
    void shouldFetchAllEmployeesSuccessfully() {
      // [Given]
      given(authApi.fetchToken()).willReturn(token);

      HrmosUser rawUser = mock(HrmosUser.class);
      List<HrmosUser> rawList = List.of(rawUser);
      given(userApi.fetchUsers(token, 1)).willReturn(rawList);

      Employee employee = mock(Employee.class);
      List<Employee> expectedEmployees = List.of(employee);
      given(mapper.toDomainList(rawList)).willReturn(expectedEmployees);

      // [When]
      List<Employee> actualEmployees = adapter.fetchAll();

      // [Then]
      assertAll(
          () -> assertNotNull(actualEmployees, "返却リストが null でないこと"),
          () -> assertEquals(1, actualEmployees.size(), "件数が一致すること"),
          () -> assertEquals(expectedEmployees, actualEmployees, "取得結果が一致すること")
      );
      then(authApi).should().fetchToken();
      then(userApi).should().fetchUsers(token, 1);
      then(mapper).should().toDomainList(rawList);
    }
  }
}
