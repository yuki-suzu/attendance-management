package com.computer_rescuer.attendance_management.adapter.in.web;

import com.computer_rescuer.attendance_management.adapter.in.model.ApiResponse;
import com.computer_rescuer.attendance_management.application.port.in.SyncEmployeeUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 従業員マスターの同期要求を受け付けるWebアダプター（Controller）。
 * <p>
 * 外部（システム管理者など）からのHTTPリクエストを受け取り、 アプリケーション層の入力ポート（{@link SyncEmployeeUseCase}）を呼び出します。
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/employees")
@RequiredArgsConstructor
public class EmployeeSyncController {

  private final SyncEmployeeUseCase syncEmployeeUseCase;

  /**
   * 外部システム（HRMOS）から従業員マスターを同期（洗い替え）します。
   *
   * @return 処理結果のHTTPレスポンス
   */
  @PostMapping("/sync")
  public ResponseEntity<ApiResponse<Void>> syncEmployees() {
    log.info("API要求受信: 従業員マスターの同期処理を開始します。");

    syncEmployeeUseCase.syncEmployees();

    log.info("API要求完了: 従業員マスターの同期処理が正常終了しました。");
    return ResponseEntity.ok(ApiResponse.success());
  }
}
