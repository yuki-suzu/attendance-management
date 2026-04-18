package com.computer_rescuer.attendance_management.adapter.out.persistence;

import static com.computer_rescuer.attendance_management.generated.jooq.Tables.M_EMPLOYEE;

import com.computer_rescuer.attendance_management.application.port.out.ResolveHrmosUserIdPort;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

/**
 * 従業員ID解決ポート（{@link ResolveHrmosUserIdPort}）の jOOQ 実装アダプター。
 * <p>
 * ローカルデータベースの従業員マスタ（m_employee）を検索し、 ドメインの従業員ID（社員番号）と、HRMOSの user_id をマッピングします。
 * </p>
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class EmployeeIdMappingAdapter implements ResolveHrmosUserIdPort {

  private final DSLContext dsl;

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<Integer> resolve(String employeeNumber) {
    if (!StringUtils.hasText(employeeNumber)) {
      return Optional.empty();
    }

    log.debug("社員番号 '{}' から HRMOS の user_id を解決します。", employeeNumber);

    Integer hrmosUserId = dsl.select(M_EMPLOYEE.ID)
        .from(M_EMPLOYEE)
        .where(M_EMPLOYEE.EMPLOYEE_NUMBER.eq(employeeNumber))
        .fetchOne(M_EMPLOYEE.ID);

    if (hrmosUserId == null) {
      log.warn("社員番号 '{}' に紐づく HRMOS の user_id がローカルDBに見つかりませんでした。",
          employeeNumber);
    }

    return Optional.ofNullable(hrmosUserId);
  }
}
