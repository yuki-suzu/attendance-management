package com.computer_rescuer.attendance_management.adapter.out.persistence.mapper;

import com.computer_rescuer.attendance_management.domain.model.Department;
import com.computer_rescuer.attendance_management.generated.jooq.tables.records.MDepartmentRecord;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 部門ドメインモデルからjOOQレコードへのマッピングを担当するマッパー。
 * <p>
 * 部門(M_DEPARTMENT)テーブル固有のデータ変換ロジックをカプセル化します。
 * </p>
 */
@Component
public class DepartmentJooqMapper {

  /**
   * 部門ドメインモデルのリストをjOOQレコードのリストに変換します。
   *
   * @param departments 変換対象の部門ドメインモデルリスト
   * @return M_DEPARTMENTテーブル用のjOOQレコードリスト
   */
  public List<MDepartmentRecord> toRecords(List<Department> departments) {
    return departments.stream()
        .map(d -> {
          MDepartmentRecord r = new MDepartmentRecord();
          r.setId(d.id());
          r.setName(d.name());
          r.setSequence(d.sequence());
          return r;
        })
        .toList();
  }
}
