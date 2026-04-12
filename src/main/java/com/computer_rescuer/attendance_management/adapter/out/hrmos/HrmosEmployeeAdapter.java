package com.computer_rescuer.attendance_management.adapter.out.hrmos;

import com.computer_rescuer.attendance_management.adapter.out.hrmos.client.HrmosAuthApi;
import com.computer_rescuer.attendance_management.adapter.out.hrmos.client.HrmosUserApi;
import com.computer_rescuer.attendance_management.adapter.out.hrmos.mapper.HrmosUserMapper;
import com.computer_rescuer.attendance_management.adapter.out.hrmos.model.HrmosUser;
import com.computer_rescuer.attendance_management.application.port.out.FetchEmployeePort;
import com.computer_rescuer.attendance_management.domain.model.Employee;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * HRMOS API を介して従業員情報を取得する出力アダプター。
 * <p>
 * アプリケーション層から定義された {@link FetchEmployeePort} を実装し、 認証リソース ({@link HrmosAuthApi}) とユーザーリソース
 * ({@link HrmosUserApi}) を 組み合わせて最新の従業員一覧を提供します。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HrmosEmployeeAdapter implements FetchEmployeePort {

  private final HrmosAuthApi authApi;
  private final HrmosUserApi userApi;
  private final HrmosUserMapper mapper;

  /**
   * HRMOS から全従業員情報を取得し、ドメインモデルのリストとして返却します。
   * <p>
   * 内部で HRMOS 認証 API を呼び出してアクセストークンを取得し、 それを用いてユーザー一覧 API からデータを収集、最終的にドメイン形式へ変換します。
   * </p>
   *
   * @return 従業員ドメインモデルのリスト。データが存在しない場合は空のリストを返却。
   * @throws com.computer_rescuer.attendance_management.adapter.out.exception.ExternalIntegrationException 外部
   *                                                                                                       API
   *                                                                                                       との通信エラー、または認証失敗時にスローされます。
   */
  @Override
  public List<Employee> fetchAll() {
    log.info("HRMOS アダプター経由で従業員情報の全件取得を開始します。");

    // 1. 認証リソースからトークンを取得
    String token = authApi.fetchToken();

    // 2. ユーザーリソースから生データを取得
    List<HrmosUser> hrmosUsers = userApi.fetchUsers(token);

    // 3. ドメインモデルへマッピングして返却
    List<Employee> employees = mapper.toDomainList(hrmosUsers);

    log.info("HRMOS から {} 件の従業員情報をドメインモデルへ変換しました。", employees.size());
    return employees;
  }
}
