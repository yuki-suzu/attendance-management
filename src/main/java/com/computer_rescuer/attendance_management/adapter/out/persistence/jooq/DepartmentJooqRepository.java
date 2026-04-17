package com.computer_rescuer.attendance_management.adapter.out.persistence.jooq;

import static com.computer_rescuer.attendance_management.generated.jooq.Tables.M_DEPARTMENT;

import com.computer_rescuer.attendance_management.generated.jooq.tables.records.MDepartmentRecord;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

/**
 * 部門マスタ(M_DEPARTMENT)テーブルに対する、jOOQを用いた物理データアクセスを担当するリポジトリ。
 */
@Repository
@RequiredArgsConstructor
public class DepartmentJooqRepository {

  private final DSLContext dsl;

  /**
   * テーブルの全レコードを物理削除します。
   */
  public void deleteAll() {
    dsl.deleteFrom(M_DEPARTMENT).execute();
  }

  /**
   * jOOQレコードのリストを一括登録します。
   *
   * @param records 登録対象の部門レコードリスト
   */
  public void batchInsert(List<MDepartmentRecord> records) {
    dsl.batchInsert(records).execute();
  }
}
