package com.computer_rescuer.attendance_management.adapter.out.persistence.jooq;

import static com.computer_rescuer.attendance_management.generated.jooq.Tables.T_CHECKED_EMPLOYEE;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

/**
 * チェック済み従業員テーブル（{@code T_CHECKED_EMPLOYEE}）に対する低レベルな SQL 実行を担当する jOOQ リポジトリ。
 * <p>
 * 高速な重複抑止インサート（ON DUPLICATE KEY IGNORE）および月次集計クエリを発行します。
 * </p>
 */
@Repository
@RequiredArgsConstructor
public class CheckedEmployeeJooqRepository {

  private final DSLContext dsl;

  /**
   * 指定日において既に記録済みの従業員IDを検索します。
   *
   * @param targetDate  対象日
   * @param employeeIds 対象従業員IDリスト
   * @return 該当する従業員IDのセット
   */
  public Set<Integer> selectCheckedEmployeeIds(LocalDate targetDate, List<Integer> employeeIds) {
    if (employeeIds == null || employeeIds.isEmpty()) {
      return Set.of();
    }

    return dsl.select(T_CHECKED_EMPLOYEE.EMPLOYEE_ID)
        .from(T_CHECKED_EMPLOYEE)
        .where(T_CHECKED_EMPLOYEE.TARGET_DATE.eq(targetDate))
        .and(T_CHECKED_EMPLOYEE.EMPLOYEE_ID.in(employeeIds))
        .fetchSet(T_CHECKED_EMPLOYEE.EMPLOYEE_ID);
  }

  /**
   * 指定月（月初から当日まで）の未打刻累積回数を従業員ごとに集計します。
   *
   * @param targetDate  対象日
   * @param employeeIds 対象従業員IDリスト
   * @return 従業員IDと当月累積回数のマップ
   */
  public Map<Integer, Integer> countMonthlyChecked(LocalDate targetDate,
      List<Integer> employeeIds) {
    if (employeeIds == null || employeeIds.isEmpty()) {
      return Map.of();
    }

    LocalDate startOfMonth = targetDate.withDayOfMonth(1);

    return dsl.select(T_CHECKED_EMPLOYEE.EMPLOYEE_ID, DSL.count())
        .from(T_CHECKED_EMPLOYEE)
        .where(T_CHECKED_EMPLOYEE.EMPLOYEE_ID.in(employeeIds))
        .and(T_CHECKED_EMPLOYEE.TARGET_DATE.between(startOfMonth, targetDate))
        .groupBy(T_CHECKED_EMPLOYEE.EMPLOYEE_ID)
        .fetchMap(T_CHECKED_EMPLOYEE.EMPLOYEE_ID, r -> r.get(DSL.count()));
  }

  /**
   * 対象従業員群をチェック済みテーブルに一括挿入します。
   * <p>
   * 主キー衝突（target_date, employee_id）時はスルーさせ、多重実行時の整合性を保証します。
   * </p>
   *
   * @param targetDate  対象日
   * @param employeeIds 従業員IDリスト
   * @param reasonCode  完了事由コード
   */
  public void batchInsertIgnoreDuplicates(LocalDate targetDate, List<Integer> employeeIds,
      String reasonCode) {
    if (employeeIds == null || employeeIds.isEmpty()) {
      return;
    }

    var queries = employeeIds.stream()
        .distinct()
        .map(id -> dsl.insertInto(T_CHECKED_EMPLOYEE)
            .set(T_CHECKED_EMPLOYEE.TARGET_DATE, targetDate)
            .set(T_CHECKED_EMPLOYEE.EMPLOYEE_ID, id)
            .set(T_CHECKED_EMPLOYEE.REASON_CODE, reasonCode)
            .onDuplicateKeyIgnore())
        .toList();

    dsl.batch(queries).execute();
  }

  /**
   * 指定日より前に作成された古いレコードを物理削除します。
   *
   * @param thresholdDate 境界日
   * @return 削除件数
   */
  public int deleteOlderThan(LocalDate thresholdDate) {
    return dsl.deleteFrom(T_CHECKED_EMPLOYEE)
        .where(T_CHECKED_EMPLOYEE.TARGET_DATE.lt(thresholdDate))
        .execute();
  }
}
