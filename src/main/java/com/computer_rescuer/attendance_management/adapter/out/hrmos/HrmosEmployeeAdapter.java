package com.computer_rescuer.attendance_management.adapter.out.hrmos;

import com.computer_rescuer.attendance_management.adapter.out.hrmos.client.HrmosApiClient;
import com.computer_rescuer.attendance_management.adapter.out.hrmos.mapper.HrmosUserMapper;
import com.computer_rescuer.attendance_management.adapter.out.hrmos.model.HrmosUser;
import com.computer_rescuer.attendance_management.application.port.out.FetchEmployeePort;
import com.computer_rescuer.attendance_management.domain.model.Employee;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * HRMOS APIを用いて従業員情報を取得する出力アダプターの実装クラス。
 * <p>
 * {@link FetchEmployeePort} を実装し、アプリケーション層の要求に応じて HRMOS固有のAPIクライアント（{@link HrmosApiClient}）を呼び出します。
 * 取得したデータのドメインモデルへの変換は {@link HrmosUserMapper} に委譲（腐敗防止層）します。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HrmosEmployeeAdapter implements FetchEmployeePort {

  private final HrmosApiClient apiClient;
  private final HrmosUserMapper userMapper;

  @Override
  public List<Employee> fetchAllEmployees() {
    log.info("HRMOSアダプター経由で従業員情報を取得します...");

    // 1. HRMOS APIから一時Tokenを取得
    String token = apiClient.fetchToken();

    // 2. Tokenを使用してユーザー一覧を取得
    List<HrmosUser> hrmosUsers = apiClient.fetchUsers(token);

    // 3. Mapperに委譲して、HRMOSモデルからドメインモデルへ一括変換
    return hrmosUsers.stream()
        .map(userMapper::toDomain)
        .toList();
  }
}
