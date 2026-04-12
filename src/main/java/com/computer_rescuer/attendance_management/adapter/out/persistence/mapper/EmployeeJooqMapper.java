package com.computer_rescuer.attendance_management.adapter.out.persistence.mapper;

import com.computer_rescuer.attendance_management.domain.model.Employee;
import com.computer_rescuer.attendance_management.generated.jooq.tables.records.MEmployeeRecord;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * ドメインモデル（Employee）を、永続化レイヤー（jOOQレコード）へ変換するマッパー。
 * <p>
 * アプリケーションのコアであるドメインモデルと、データベースの物理構造の差異を吸収します。
 * このクラスは特定の外部システム（HRMOSなど）の存在を知らず、純粋にドメインとDBの橋渡しのみを行います。
 * </p>
 */
@Component
public class EmployeeJooqMapper {

  /**
   * ドメインの従業員情報リストを、M_EMPLOYEEテーブル用のjOOQレコードリストに変換します。
   * <p>
   * データベース側の created_at, updated_at は、SQL実行時にDB側のデフォルト値 (DEFAULT CURRENT_TIMESTAMP)
   * を使用するため、ここでは値をセットしません。
   * </p>
   *
   * @param employees 永続化対象となるドメインモデルの従業員リスト
   * @return データベースへの一括登録に使用可能な MEmployeeRecord のリスト
   */
  public List<MEmployeeRecord> toRecords(List<Employee> employees) {
    return employees.stream()
        .map(e -> {
          MEmployeeRecord record = new MEmployeeRecord();

          record.setId(e.id());
          record.setEmployeeNumber(e.employeeNumber());
          record.setLastName(e.lastName());
          record.setFirstName(e.firstName());
          record.setEmail(e.email());
          record.setDepartmentId(e.departmentId());
          record.setDefaultSegmentId(e.defaultSegmentId());
          record.setEmploymentId(e.employmentId());

          return record;
        })
        .toList();
  }
}
