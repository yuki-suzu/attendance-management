package com.computer_rescuer.attendance_management.adapter.in.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;

import com.computer_rescuer.attendance_management.adapter.in.model.ApiResponse;
import com.computer_rescuer.attendance_management.application.port.in.SyncMasterDataUseCase;
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
 * {@link MasterDataSyncController} の単体テストクラス。
 * <p>
 * マスタデータ手動同期 Web アダプターとして、入力ポート（{@link SyncMasterDataUseCase}）の呼び出し および HTTP 200 (OK)
 * レスポンス返却の命令網羅（C0 100%）を検証します。 Spring コンテキストを起動せず、純粋な Mockito 単体テストとして高速に実行します。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MasterDataSyncController 単体テスト")
class MasterDataSyncControllerTest {

  @Mock
  private SyncMasterDataUseCase syncMasterDataUseCase;

  @InjectMocks
  private MasterDataSyncController controller;

  @Nested
  @DisplayName("マスタデータ同期（syncMasterData）シナリオ")
  class SyncMasterDataScenario {

    /**
     * 同期要求を受け取った際、ユースケースが実行され、HTTP 200 (OK) が返却されることを検証します。
     */
    @Test
    @DisplayName("正常系: マスタ同期ユースケースが実行され、200 OK レスポンスが返却されること")
    void shouldExecuteSyncMasterDataAndReturnOkResponse() {
      // [Given]
      willDoNothing().given(syncMasterDataUseCase).syncMasterData();

      // [When]
      ResponseEntity<ApiResponse<Void>> response = controller.syncMasterData();

      // [Then]
      assertEquals(HttpStatus.OK, response.getStatusCode(), "HTTP ステータスは 200 であること");
      assertNull(response.getBody().data(), "レスポンスボディは null であること");
      then(syncMasterDataUseCase).should().syncMasterData();
    }
  }
}
