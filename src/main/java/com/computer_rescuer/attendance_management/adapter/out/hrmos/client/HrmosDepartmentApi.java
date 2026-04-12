package com.computer_rescuer.attendance_management.adapter.out.hrmos.client;

import com.computer_rescuer.attendance_management.adapter.out.hrmos.model.HrmosDepartment;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * HRMOS 部門（Departments）APIを呼び出すクライアント。
 * <p>
 * エンドポイント: /departments
 * </p>
 */
@Component
@RequiredArgsConstructor
public class HrmosDepartmentApi {

  private final HrmosCoreHttpClient coreClient;

  /**
   * HRMOSからすべての部門情報を取得します。
   *
   * @param token API通信用の有効なアクセストークン
   * @return 部門モデルのリスト
   */
  public List<HrmosDepartment> fetchDepartments(String token) {
    return coreClient.fetchAndParseList(
        token, "/departments", "departments", "部門", new TypeReference<>() {
        }
    );
  }
}
