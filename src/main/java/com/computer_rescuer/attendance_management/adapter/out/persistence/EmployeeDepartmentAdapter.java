package com.computer_rescuer.attendance_management.adapter.out.persistence;

import static com.computer_rescuer.attendance_management.generated.jooq.Tables.M_DEPARTMENT;
import static com.computer_rescuer.attendance_management.generated.jooq.Tables.M_EMPLOYEE;

import com.computer_rescuer.attendance_management.application.port.out.FetchEmployeeDepartmentPort;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

/**
 * 従業員所属取得ポート（{@link FetchEmployeeDepartmentPort}）の JDBC (jOOQ) 実装アダプター。
 * <p>
 * ローカルデータベース上の従業員マスタ（m_employee）と部門マスタ（m_department）を JOIN して、正確な部門名を一括取得します。
 * </p>
 */
@Repository
@RequiredArgsConstructor
public class EmployeeDepartmentAdapter implements FetchEmployeeDepartmentPort {

  private final DSLContext dsl;

  /**
   * {@inheritDoc}
   * <p>
   * M_EMPLOYEE と M_DEPARTMENT を LEFT JOIN して情報を取得します。<br> 該当する部門が存在しない（DEPARTMENT_IDがnullなど）場合は
   * "未所属" を返却します。
   * </p>
   */
  @Override
  public Map<Integer, String> fetchDepartmentMapByUserIds(List<Integer> userIds) {
    if (userIds == null || userIds.isEmpty()) {
      return Map.of();
    }

    return dsl.select(M_EMPLOYEE.ID, M_DEPARTMENT.NAME)
        .from(M_EMPLOYEE)
        .leftJoin(M_DEPARTMENT).on(M_EMPLOYEE.DEPARTMENT_ID.eq(M_DEPARTMENT.ID))
        .where(M_EMPLOYEE.ID.in(userIds))
        .fetchMap(
            M_EMPLOYEE.ID,
            r -> r.get(M_DEPARTMENT.NAME) != null ? r.get(M_DEPARTMENT.NAME) : "未所属"
        );
  }
}
