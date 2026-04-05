package com.computer_rescuer.attendance_management.application.port.out;

import com.computer_rescuer.attendance_management.domain.model.Employee;
import java.util.List;

/**
 * 従業員情報を永続化（DB保存など）するための出力ポート。
 */
public interface EmployeeRepositoryPort {

  /**
   * 登録されているすべての従業員情報を削除します（洗い替え用）。
   */
  void deleteAllEmployees();

  /**
   * 従業員情報を一括で登録します。
   *
   * @param employees 登録する従業員ドメインモデルのリスト
   */
  void saveAll(List<Employee> employees);
}
