package com.computer_rescuer.attendance_management.adapter.out.persistence;

import static com.computer_rescuer.attendance_management.generated.jooq.Tables.M_EMPLOYEE;

import com.computer_rescuer.attendance_management.application.port.out.FetchEmployeeByIdPort;
import com.computer_rescuer.attendance_management.domain.model.Employee;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

/**
 * 従業員情報取得ポート（{@link FetchEmployeeByIdPort}）の JDBC (jOOQ) 実装アダプター。
 * <p>
 * ローカルデータベース上の従業員マスタ（m_employee）を検索し、ドメインモデルである Employee へマッピングして返却します。
 * </p>
 */
@Repository
@RequiredArgsConstructor
public class EmployeeByIdAdapter implements FetchEmployeeByIdPort {

  private final DSLContext dsl;

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<Integer, Employee> fetchEmployeeMapByUserIds(List<Integer> userIds) {
    if (userIds == null || userIds.isEmpty()) {
      return Map.of();
    }

    return dsl.select(
            M_EMPLOYEE.ID,
            M_EMPLOYEE.EMPLOYEE_NUMBER,
            M_EMPLOYEE.LAST_NAME,
            M_EMPLOYEE.FIRST_NAME,
            M_EMPLOYEE.EMAIL,
            M_EMPLOYEE.DEPARTMENT_ID,
            M_EMPLOYEE.DEFAULT_SEGMENT_ID,
            M_EMPLOYEE.EMPLOYMENT_ID
        )
        .from(M_EMPLOYEE)
        .where(M_EMPLOYEE.ID.in(userIds))
        .fetchMap(
            M_EMPLOYEE.ID,
            r -> new Employee(
                r.get(M_EMPLOYEE.ID),
                r.get(M_EMPLOYEE.EMPLOYEE_NUMBER),
                r.get(M_EMPLOYEE.LAST_NAME),
                r.get(M_EMPLOYEE.FIRST_NAME),
                r.get(M_EMPLOYEE.EMAIL),
                r.get(M_EMPLOYEE.DEPARTMENT_ID),
                r.get(M_EMPLOYEE.DEFAULT_SEGMENT_ID),
                r.get(M_EMPLOYEE.EMPLOYMENT_ID)
            )
        );
  }
}
