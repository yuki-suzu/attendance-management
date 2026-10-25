package com.computer_rescuer.attendance_management.adapter.in.web;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;

import com.computer_rescuer.attendance_management.adapter.in.model.ApiResponse;
import com.computer_rescuer.attendance_management.adapter.in.model.ResultCode;
import com.computer_rescuer.attendance_management.application.port.in.SyncEmployeeUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * {@link EmployeeSyncController} の単体テストクラス。
 * <p>
 * 従業員マスタ手動同期 Web アダプターとして、入力ポート（{@link SyncEmployeeUseCase}）の呼び出しおよび
 * 統一レスポンスフォーマット（{@link ApiResponse}）返却の命令網羅（C0 100%）を検証します。 Spring コンテキストを起動せず、純粋な Mockito
 * 単体テストとして高速に実行します。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmployeeSyncController 単体テスト")
class EmployeeSyncControllerTest {

  @Mock
  private SyncEmployeeUseCase syncEmployeeUseCase;

  @InjectMocks
  private EmployeeSyncController controller;

  @Nested
  @DisplayName("従業員マスタ同期（syncEmployees）シナリオ")
  class SyncEmployeesScenario {

    /**
     * 同期要求を受け取った際、ユースケースが実行され、HTTP 200 (OK) と 成功を示す {@link ApiResponse} が返却されることを検証します。
     */
    @Test
    @DisplayName("正常系: 従業員同期ユースケースが実行され、200 OK と成功レスポンスが返却されること")
    void shouldExecuteSyncEmployeesAndReturnSuccessResponse() {
      // [Given]
      willDoNothing().given(syncEmployeeUseCase).syncEmployees();

      // [When]
      ResponseEntity<ApiResponse<Void>> response = controller.syncEmployees();

      // [Then]
      assertAll(
          () -> assertEquals(HttpStatus.OK, response.getStatusCode(),
              "HTTP ステータスは 200 であること"),
          () -> assertNotNull(response.getBody(), "レスポンスボディが存在すること"),
          () -> assertEquals(ResultCode.SUCCESS.name(), response.getBody().code(),
              "成功コードが返却されること")
      );
      then(syncEmployeeUseCase).should().syncEmployees();
    }
  }
}
