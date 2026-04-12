package com.computer_rescuer.attendance_management.adapter.out.persistence.jooq;

import static com.computer_rescuer.attendance_management.generated.jooq.Tables.M_EMPLOYEE;

import com.computer_rescuer.attendance_management.generated.jooq.tables.records.MEmployeeRecord;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

/**
 * jOOQを利用した従業員テーブル(M_EMPLOYEE)専用のデータアクセスオブジェクト。
 * <p>
 * 標準的なCRUD操作以外の、特にパフォーマンスが要求される一括処理や、 複雑なSQL実行が必要なインフラストラクチャ操作を担当します。
 * </p>
 */
@Repository
@RequiredArgsConstructor
public class EmployeeJooqRepository {

  private final DSLContext dsl;

  /**
   * 従業員マスタ(M_EMPLOYEE)の全レコードを物理削除します。
   */
  public void deleteAll() {
    dsl.deleteFrom(M_EMPLOYEE).execute();
  }

  /**
   * 指定されたjOOQレコードのリストを用いて、データベースへ一括登録(Batch Insert)を実行します。
   * <p>
   * レコード内のフィールド値に基づき、jOOQが最適化された単一のマルチバリューINSERT文、 またはJDBCバッチアップデートを生成して実行します。
   * </p>
   *
   * @param records 登録対象の MEmployeeRecord リスト
   */
  public void batchInsert(List<MEmployeeRecord> records) {
    // セットされた値のみをINSERT対象にするため、カラム順序の不一致や欠落に強い
    dsl.batchInsert(records).execute();
  }
}
