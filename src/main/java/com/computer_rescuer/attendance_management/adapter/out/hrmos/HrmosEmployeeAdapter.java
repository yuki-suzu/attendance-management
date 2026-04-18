package com.computer_rescuer.attendance_management.adapter.out.hrmos;

import com.computer_rescuer.attendance_management.adapter.out.hrmos.client.HrmosAuthApi;
import com.computer_rescuer.attendance_management.adapter.out.hrmos.client.HrmosUserApi;
import com.computer_rescuer.attendance_management.adapter.out.hrmos.mapper.HrmosUserMapper;
import com.computer_rescuer.attendance_management.adapter.out.hrmos.model.HrmosUser;
import com.computer_rescuer.attendance_management.adapter.out.hrmos.support.HrmosPaginationHelper;
import com.computer_rescuer.attendance_management.application.port.out.FetchEmployeePort;
import com.computer_rescuer.attendance_management.domain.model.Employee;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class HrmosEmployeeAdapter implements FetchEmployeePort {

  private final HrmosAuthApi authApi;
  private final HrmosUserApi userApi;
  private final HrmosUserMapper mapper;

  // 💡 修正後
  @Override
  public List<Employee> fetchAll() {
    log.info("HRMOS アダプター経由で従業員情報の全件取得を開始します。");
    String token = authApi.fetchToken();

    // 💡 ヘルパーに委譲！数十行のループがたった3行に。
    List<HrmosUser> allHrmosUsers = HrmosPaginationHelper.fetchAllPages("従業員情報", page ->
        userApi.fetchUsers(token, page)
    );

    List<Employee> employees = mapper.toDomainList(allHrmosUsers);
    log.info("HRMOS から合計 {} 件の従業員情報を取得し、ドメインモデルへ変換しました。",
        employees.size());
    return employees;
  }
}
