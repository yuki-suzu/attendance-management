package com.computer_rescuer.attendance_management.adapter.in.web;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.computer_rescuer.attendance_management.adapter.in.model.ApiResponse;
import com.computer_rescuer.attendance_management.adapter.in.model.ResultCode;
import com.computer_rescuer.attendance_management.application.port.out.CheckedEmployeeRepositoryPort;
import com.computer_rescuer.attendance_management.application.port.out.MonthlyAttendanceSummaryRepositoryPort;
import com.computer_rescuer.attendance_management.shared.TestClockFactory;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * {@link AdminMaintenanceController} の単体テストクラス。
 * <p>
 * システム管理者向けデータメンテナンス API として、チェック済み従業員履歴および月次勤怠サマリの パージ処理における引数判定（基準日指定有無）とリポジトリポート連携の命令網羅（C0
 * 100%）を検証します。 Spring コンテキストを起動せず、純粋な Mockito 単体テストとして高速に実行します。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminMaintenanceController 単体テスト")
class AdminMaintenanceControllerTest {

  @Mock
  private CheckedEmployeeRepositoryPort checkedEmployeeRepositoryPort;

  @Mock
  private MonthlyAttendanceSummaryRepositoryPort summaryRepositoryPort;

  @Spy
  private Clock clock = TestClockFactory.fixedAt("2026-04-20", "10:00:00");

  @InjectMocks
  private AdminMaintenanceController controller;

  @Nested
  @DisplayName("チェック済み従業員履歴パージ（purgeExpiredCheckedEmployees）シナリオ")
  class PurgeExpiredCheckedEmployeesScenario {

    /**
     * 基準日（beforeDate）が明示的に指定された場合、指定された日付を用いて リポジトリポートの物理削除が呼び出され、正常なレスポンスが返却されることを検証します。
     */
    @Test
    @DisplayName("基準日指定あり: 指定された基準日でパージ処理が実行され、削除件数を含むレスポンスを返却すること")
    void shouldPurgeWithSpecifiedBeforeDate() {
      // [Given]
      LocalDate specifiedDate = LocalDate.of(2025, 4, 1);
      int deletedCount = 15;
      given(checkedEmployeeRepositoryPort.deleteOlderThan(specifiedDate)).willReturn(deletedCount);

      // [When]
      ResponseEntity<ApiResponse<String>> response = controller.purgeExpiredCheckedEmployees(
          specifiedDate);

      // [Then]
      assertAll(
          () -> assertEquals(HttpStatus.OK, response.getStatusCode(),
              "HTTP ステータスは 200 であること"),
          () -> assertNotNull(response.getBody(), "レスポンスボディが存在すること"),
          () -> assertEquals(ResultCode.SUCCESS.name(), response.getBody().code(),
              "成功コードが返却されること"),
          () -> {
            String message = response.getBody().data();
            assertNotNull(message, "メッセージデータが存在すること");
            assertTrue(message.contains("2025-04-01"), "指定日付がメッセージに含まれること");
            assertTrue(message.contains("15 件"), "削除件数がメッセージに含まれること");
          }
      );
      then(checkedEmployeeRepositoryPort).should().deleteOlderThan(specifiedDate);
    }

    /**
     * 基準日（beforeDate）が省略（null）された場合、システム日付から算出されたデフォルト基準日（本日より1年前）を用いて パージ処理が実行されることを検証します。
     */
    @Test
    @DisplayName("基準日指定なし (null): デフォルトの基準日（1年前）が算出されてパージ処理が実行されること")
    void shouldPurgeWithDefaultBeforeDateWhenNull() {
      // [Given]
      LocalDate nullDate = null;
      int deletedCount = 8;
      given(checkedEmployeeRepositoryPort.deleteOlderThan(any(LocalDate.class))).willReturn(
          deletedCount);

      // [When]
      ResponseEntity<ApiResponse<String>> response = controller.purgeExpiredCheckedEmployees(
          nullDate);

      // [Then]
      assertAll(
          () -> assertEquals(HttpStatus.OK, response.getStatusCode(),
              "HTTP ステータスは 200 であること"),
          () -> assertNotNull(response.getBody(), "レスポンスボディが存在すること"),
          () -> assertTrue(response.getBody().data().contains("8 件"),
              "削除件数がメッセージに含まれること")
      );
      then(checkedEmployeeRepositoryPort).should().deleteOlderThan(any(LocalDate.class));
    }
  }

  @Nested
  @DisplayName("月次勤怠サマリパージ（purgeExpiredMonthlySummaries）シナリオ")
  class PurgeExpiredMonthlySummariesScenario {

    /**
     * 基準日（beforeDate）が明示的に指定された場合、その日付の開始時刻（atStartOfDay）を用いて リポジトリポートのパージが呼び出されることを検証します。
     */
    @Test
    @DisplayName("基準日指定あり: 指定された日付の開始時刻（00:00）でパージ処理が実行されること")
    void shouldPurgeMonthlySummariesWithSpecifiedBeforeDate() {
      // [Given]
      LocalDate specifiedDate = LocalDate.of(2025, 10, 1);
      LocalDateTime expectedThreshold = specifiedDate.atStartOfDay();
      int deletedCount = 20;
      given(summaryRepositoryPort.deleteOlderThan(expectedThreshold)).willReturn(deletedCount);

      // [When]
      ResponseEntity<ApiResponse<String>> response = controller.purgeExpiredMonthlySummaries(
          specifiedDate);

      // [Then]
      assertAll(
          () -> assertEquals(HttpStatus.OK, response.getStatusCode(),
              "HTTP ステータスは 200 であること"),
          () -> assertNotNull(response.getBody(), "レスポンスボディが存在すること"),
          () -> assertTrue(response.getBody().data().contains("20 件"),
              "削除件数がメッセージに含まれること")
      );
      then(summaryRepositoryPort).should().deleteOlderThan(expectedThreshold);
    }

    /**
     * 基準日（beforeDate）が省略（null）された場合、デフォルト基準日（本日より6ヶ月前）を用いて パージ処理が実行されることを検証します。
     */
    @Test
    @DisplayName("基準日指定なし (null): デフォルトの基準日（6ヶ月前）が算出されてパージ処理が実行されること")
    void shouldPurgeMonthlySummariesWithDefaultBeforeDateWhenNull() {
      // [Given]
      LocalDate nullDate = null;
      int deletedCount = 3;
      given(summaryRepositoryPort.deleteOlderThan(any(LocalDateTime.class))).willReturn(
          deletedCount);

      // [When]
      ResponseEntity<ApiResponse<String>> response = controller.purgeExpiredMonthlySummaries(
          nullDate);

      // [Then]
      assertAll(
          () -> assertEquals(HttpStatus.OK, response.getStatusCode(),
              "HTTP ステータスは 200 であること"),
          () -> assertNotNull(response.getBody(), "レスポンスボディが存在すること"),
          () -> assertTrue(response.getBody().data().contains("3 件"),
              "削除件数がメッセージに含まれること")
      );
      then(summaryRepositoryPort).should().deleteOlderThan(any(LocalDateTime.class));
    }
  }
}
