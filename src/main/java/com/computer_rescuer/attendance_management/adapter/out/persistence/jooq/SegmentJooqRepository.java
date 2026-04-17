package com.computer_rescuer.attendance_management.adapter.out.persistence.jooq;

import static com.computer_rescuer.attendance_management.generated.jooq.Tables.M_SEGMENT;

import com.computer_rescuer.attendance_management.generated.jooq.tables.records.MSegmentRecord;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

/**
 * 勤務区分マスタ(M_SEGMENT)テーブルに対する、jOOQを用いた物理データアクセスを担当するリポジトリ。
 */
@Repository
@RequiredArgsConstructor
public class SegmentJooqRepository {

  private final DSLContext dsl;

  /**
   * テーブルの全レコードを物理削除します。
   */
  public void deleteAll() {
    dsl.deleteFrom(M_SEGMENT).execute();
  }

  /**
   * jOOQレコードのリストを一括登録します。
   *
   * @param records 登録対象の勤務区分レコードリスト
   */
  public void batchInsert(List<MSegmentRecord> records) {
    dsl.batchInsert(records).execute();
  }
}
