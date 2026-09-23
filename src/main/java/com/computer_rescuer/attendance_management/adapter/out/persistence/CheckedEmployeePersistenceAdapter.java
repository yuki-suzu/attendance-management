package com.computer_rescuer.attendance_management.adapter.out.persistence;

import com.computer_rescuer.attendance_management.adapter.out.persistence.jooq.CheckedEmployeeJooqRepository;
import com.computer_rescuer.attendance_management.application.port.out.CheckedEmployeeRepositoryPort;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * チェック済み従業員情報出力ポート（{@link CheckedEmployeeRepositoryPort}）の永続化実装アダプター。
 * <p>
 * {@link CheckedEmployeeJooqRepository} を制御し、未打刻チェック履歴の問い合わせおよび永続化を実行します。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CheckedEmployeePersistenceAdapter implements CheckedEmployeeRepositoryPort {

  private final CheckedEmployeeJooqRepository jooqRepository;

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Integer> findCheckedEmployeeIds(LocalDate targetDate, List<Integer> employeeIds) {
    return jooqRepository.selectCheckedEmployeeIds(targetDate, employeeIds);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<Integer, Integer> countMonthlyChecked(LocalDate targetDate,
      List<Integer> employeeIds) {
    return jooqRepository.countMonthlyChecked(targetDate, employeeIds);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void saveAll(LocalDate targetDate, List<Integer> employeeIds, String reasonCode) {
    log.info("t_checked_employee へ {} 件のレコードを一括登録します（targetDate: {}, reason: {}）",
        employeeIds.size(), targetDate, reasonCode);
    jooqRepository.batchInsertIgnoreDuplicates(targetDate, employeeIds, reasonCode);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int deleteOlderThan(LocalDate thresholdDate) {
    int deleted = jooqRepository.deleteOlderThan(thresholdDate);
    log.info(
        "t_checked_employee のデータクリーンアップを実施しました（基準日: {} 以前, 削除件数: {}）",
        thresholdDate, deleted);
    return deleted;
  }
}
