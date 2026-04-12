package com.computer_rescuer.attendance_management.application.interactor;

import com.computer_rescuer.attendance_management.application.port.in.SyncEmployeeUseCase;
import com.computer_rescuer.attendance_management.application.port.out.EmployeeRepositoryPort;
import com.computer_rescuer.attendance_management.application.port.out.FetchEmployeePort;
import com.computer_rescuer.attendance_management.domain.model.Employee;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 従業員マスター同期ユースケースの実装クラス（インタラクター）。
 * <p>
 * 入力ポート（{@link SyncEmployeeUseCase}）を実装し、 出力ポート（{@link FetchEmployeePort} 等）を組み合わせてビジネスロジックを制御します。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SyncEmployeeInteractor implements SyncEmployeeUseCase {

  // 外部から従業員を取得するためのPort
  private final FetchEmployeePort fetchEmployeePort;

  // DB更新用の出力ポート
  private final EmployeeRepositoryPort employeeRepositoryPort;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void syncEmployees() {
    log.info("従業員マスターの同期処理を開始します...");

    // 1. 外部システムから全従業員を取得
    List<Employee> employees = fetchEmployeePort.fetchAll();
    log.info("外部システムから {} 件の従業員情報を取得しました。", employees.size());

    if (employees.isEmpty()) {
      log.warn("取得した従業員情報が0件のため、同期処理をスキップします。");
      return;
    }
    log.debug(employees.toString());

    // 2. DBの従業員マスタを全削除
    employeeRepositoryPort.deleteAllEmployees();

    // 3. 取得した従業員情報をDBに一括登録
    employeeRepositoryPort.saveAll(employees);

    log.info("従業員マスターの同期処理が完了しました。");
  }
}
