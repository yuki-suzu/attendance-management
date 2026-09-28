package com.computer_rescuer.attendance_management.adapter.out.hrmos;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.computer_rescuer.attendance_management.adapter.out.hrmos.client.HrmosAuthApi;
import com.computer_rescuer.attendance_management.adapter.out.hrmos.client.HrmosStampLogApi;
import com.computer_rescuer.attendance_management.adapter.out.hrmos.mapper.HrmosStampLogMapper;
import com.computer_rescuer.attendance_management.adapter.out.hrmos.model.HrmosStampLog;
import com.computer_rescuer.attendance_management.application.port.out.FetchEmployeeByIdPort;
import com.computer_rescuer.attendance_management.application.port.out.ResolveHrmosUserIdPort;
import com.computer_rescuer.attendance_management.domain.model.Employee;
import com.computer_rescuer.attendance_management.domain.model.StampLog;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link HrmosStampLogAdapter} の単体テストクラス。
 * <p>
 * 打刻ログ実績取得出力ポートの実装として、ページネーションヘルパーを介した日次打刻取得・ 社員マスタエンリッチ結合、および特定ユーザー打刻取得における社員番号解決・
 * 日付フォーマット変換連携の命令網羅（C0 100%）を検証します。 Spring コンテキストを起動せず、純粋な Mockito 単体テストとして高速に実行します。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HrmosStampLogAdapter 単体テスト")
class HrmosStampLogAdapterTest {

  @Mock
  private HrmosAuthApi authApi;

  @Mock
  private HrmosStampLogApi stampLogApi;

  @Mock
  private HrmosStampLogMapper mapper;

  @Mock
  private FetchEmployeeByIdPort employeeByIdPort;

  @Mock
  private ResolveHrmosUserIdPort resolveHrmosUserIdPort;

  @InjectMocks
  private HrmosStampLogAdapter adapter;

  private final String token = "test-auth-token";

  @Nested
  @DisplayName("日次打刻ログ取得（fetchDailyLogs）シナリオ")
  class FetchDailyLogsScenario {

    /**
     * 引数の日付が null の場合、API 通信を行わずに即座に空リストが返却されることを検証します（C0 分岐網羅）。
     */
    @Test
    @DisplayName("null 安全性: date が null の場合、早期リターンして空リストを返却すること")
    void shouldReturnEmptyListWhenDateIsNull() {
      // [When]
      List<StampLog> result = adapter.fetchDailyLogs(null);

      // [Then]
      assertAll(
          () -> assertNotNull(result),
          () -> assertTrue(result.isEmpty())
      );
      then(authApi).shouldHaveNoInteractions();
      then(stampLogApi).shouldHaveNoInteractions();
    }

    /**
     * ページネーションヘルパーを介して生打刻ログが取得され、 ユーザーIDの重複排除を経て社員情報とエンリッチ結合されることを検証します。
     */
    @Test
    @DisplayName("正常系: 日次打刻ログを取得し、ユーザーIDを重複排除して社員情報とエンリッチ結合すること")
    void shouldFetchDailyLogsAndEnrichWithEmployeeData() {
      // [Given]
      LocalDate date = LocalDate.of(2026, 4, 20);
      given(authApi.fetchToken()).willReturn(token);

      // 同一ユーザー（userId: 101）の打刻が複数件存在
      HrmosStampLog raw1 = new HrmosStampLog(101, "2026-04-20T08:30:00+09:00", 1, 1, 1, "Agent");
      HrmosStampLog raw2 = new HrmosStampLog(101, "2026-04-20T18:00:00+09:00", 2, 1, 1, "Agent");
      List<HrmosStampLog> rawList = List.of(raw1, raw2);

      given(stampLogApi.fetchDailyStampLogs(token, "2026-04-20", 1)).willReturn(rawList);

      Employee emp = mock(Employee.class);
      Map<Integer, Employee> employeeMap = Map.of(101, emp);
      // 重複排除により 1 件のみ社員情報が問い合わせられることを検証
      given(employeeByIdPort.fetchEmployeeMapByUserIds(List.of(101))).willReturn(employeeMap);

      StampLog domainLog = mock(StampLog.class);
      given(mapper.toDomainList(rawList, employeeMap)).willReturn(List.of(domainLog));

      // [When]
      List<StampLog> result = adapter.fetchDailyLogs(date);

      // [Then]
      assertAll(
          () -> assertEquals(1, result.size()),
          () -> assertEquals(domainLog, result.getFirst())
      );
      then(employeeByIdPort).should().fetchEmployeeMapByUserIds(List.of(101));
      then(mapper).should().toDomainList(rawList, employeeMap);
    }
  }

  @Nested
  @DisplayName("ユーザー別打刻ログ取得（fetchUserLogs）シナリオ")
  class FetchUserLogsScenario {

    private final String employeeNumber = "EMP001";
    private final Integer userId = 201;

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("境界値: 社員番号が無効な場合、早期リターンして空リストを返却すること")
    void shouldReturnEmptyListWhenEmployeeNumberIsBlank(String invalidNumber) {
      // [When]
      List<StampLog> result = adapter.fetchUserLogs(invalidNumber, null, null);

      // [Then]
      assertTrue(result.isEmpty());
      then(resolveHrmosUserIdPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("異常系: HRMOS ユーザーIDの解決に失敗した場合、空リストを返却すること")
    void shouldReturnEmptyListWhenUserIdResolutionFails() {
      // [Given]
      given(resolveHrmosUserIdPort.resolve(employeeNumber)).willReturn(Optional.empty());

      // [When]
      List<StampLog> result = adapter.fetchUserLogs(employeeNumber, null, null);

      // [Then]
      assertTrue(result.isEmpty());
      then(authApi).shouldHaveNoInteractions();
      then(stampLogApi).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("正常系: 期間指定ありの場合、DateTimeSupports により変換された ISO 文字列で API を実行すること")
    void shouldFetchUserLogsWithFormattedDateRange() {
      // [Given]
      LocalDate fromDate = LocalDate.of(2026, 4, 1);
      LocalDate toDate = LocalDate.of(2026, 4, 10);
      String expectedFromApi = "2026-04-01T00:00:00+09:00";
      String expectedToApi = "2026-04-11T00:00:00+09:00"; // toEndOfDay(2026-04-10) -> 翌日 00:00:00

      given(resolveHrmosUserIdPort.resolve(employeeNumber)).willReturn(Optional.of(userId));
      given(authApi.fetchToken()).willReturn(token);

      HrmosStampLog rawLog = new HrmosStampLog(userId, "2026-04-01T09:00:00+09:00", 1, 1, 1,
          "Agent");
      given(stampLogApi.fetchUserStampLogs(token, userId, expectedFromApi, expectedToApi, 1))
          .willReturn(List.of(rawLog));

      Employee employee = mock(Employee.class);
      given(employeeByIdPort.fetchEmployeeMapByUserIds(List.of(userId)))
          .willReturn(Map.of(userId, employee));

      StampLog domainLog = mock(StampLog.class);
      given(mapper.toDomainList(any(), any())).willReturn(List.of(domainLog));

      // [When]
      List<StampLog> result = adapter.fetchUserLogs(employeeNumber, fromDate, toDate);

      // [Then]
      assertAll(
          () -> assertEquals(1, result.size()),
          () -> assertEquals(domainLog, result.getFirst())
      );
      then(stampLogApi).should()
          .fetchUserStampLogs(token, userId, expectedFromApi, expectedToApi, 1);
    }

    @Test
    @DisplayName("正常系: 期間指定が null の場合、API パラメータ null で取得を実行すること")
    void shouldFetchUserLogsWithNullDates() {
      // [Given]
      given(resolveHrmosUserIdPort.resolve(employeeNumber)).willReturn(Optional.of(userId));
      given(authApi.fetchToken()).willReturn(token);

      HrmosStampLog rawLog = new HrmosStampLog(userId, "2026-04-01T09:00:00+09:00", 1, 1, 1,
          "Agent");
      given(stampLogApi.fetchUserStampLogs(token, userId, null, null, 1))
          .willReturn(List.of(rawLog));

      given(employeeByIdPort.fetchEmployeeMapByUserIds(List.of(userId)))
          .willReturn(Collections.emptyMap());
      given(mapper.toDomainList(any(), any())).willReturn(Collections.emptyList());

      // [When]
      List<StampLog> result = adapter.fetchUserLogs(employeeNumber, null, null);

      // [Then]
      assertNotNull(result);
      then(stampLogApi).should().fetchUserStampLogs(token, userId, null, null, 1);
    }

    @Test
    @DisplayName("正常系: 取得データが空の場合、社員情報取得を行わず空リストを返却すること")
    void shouldReturnEmptyListWhenApiReturnsEmpty() {
      // [Given]
      given(resolveHrmosUserIdPort.resolve(employeeNumber)).willReturn(Optional.of(userId));
      given(authApi.fetchToken()).willReturn(token);
      given(stampLogApi.fetchUserStampLogs(token, userId, null, null, 1))
          .willReturn(Collections.emptyList());

      // [When]
      List<StampLog> result = adapter.fetchUserLogs(employeeNumber, null, null);

      // [Then]
      assertTrue(result.isEmpty());
      then(employeeByIdPort).shouldHaveNoInteractions();
      then(mapper).shouldHaveNoInteractions();
    }
  }
}
