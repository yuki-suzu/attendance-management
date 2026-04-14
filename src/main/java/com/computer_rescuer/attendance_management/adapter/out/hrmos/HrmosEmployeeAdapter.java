package com.computer_rescuer.attendance_management.adapter.out.hrmos;

import com.computer_rescuer.attendance_management.adapter.out.hrmos.client.HrmosAuthApi;
import com.computer_rescuer.attendance_management.adapter.out.hrmos.client.HrmosUserApi;
import com.computer_rescuer.attendance_management.adapter.out.hrmos.mapper.HrmosUserMapper;
import com.computer_rescuer.attendance_management.adapter.out.hrmos.model.HrmosUser;
import com.computer_rescuer.attendance_management.application.port.out.FetchEmployeePort;
import com.computer_rescuer.attendance_management.domain.model.Employee;
import java.util.ArrayList;
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

  @Override
  public List<Employee> fetchAll() {
    log.info("HRMOS アダプター経由で従業員情報の全件取得を開始します。");

    String token = authApi.fetchToken();
    List<HrmosUser> allHrmosUsers = new ArrayList<>();

    // ページネーションを加味した全件取得ループ
    int page = 1;
    while (true) {
      log.debug("従業員情報を取得中... (ページ: {})", page);
      List<HrmosUser> pagedUsers = userApi.fetchUsers(token, page);

      // データが空なら終了
      if (pagedUsers == null || pagedUsers.isEmpty()) {
        break;
      }

      allHrmosUsers.addAll(pagedUsers);

      // 取得件数が上限(100件)未満なら、次ページはないと判断して終了
      if (pagedUsers.size() < 100) {
        break;
      }
      page++;
    }

    List<Employee> employees = mapper.toDomainList(allHrmosUsers);
    log.info("HRMOS から合計 {} 件の従業員情報を取得し、ドメインモデルへ変換しました。",
        employees.size());

    return employees;
  }
}
