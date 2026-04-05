package com.computer_rescuer.attendance_management.application.port.out;

import com.computer_rescuer.attendance_management.domain.model.Employee;
import java.util.List;

/**
 * 外部システムから従業員情報を取得するための出力ポート（インターフェース）。
 * <p>
 * アプリケーション層（ユースケース）は、特定の外部システム（HRMOSなど）の仕様に依存せず、 このインターフェースを通じて抽象化された従業員データを取得します。
 * </p>
 */
public interface FetchEmployeePort {

  /**
   * 外部システムに登録されているすべての従業員情報を取得します。
   *
   * @return システム共通の従業員モデル（{@link Employee}）のリスト
   */
  List<Employee> fetchAllEmployees();
}
