package com.computer_rescuer.attendance_management.adapter.out.persistence.jdbc;

import com.computer_rescuer.attendance_management.adapter.out.persistence.entity.EmployeeEntity;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;

public interface EmployeeJdbcRepository extends ListCrudRepository<EmployeeEntity, Integer> {

  /**
   * 洗い替え用の高速な全件削除処理。 標準の deleteAll() のオーバーヘッドを避けるため、直接 DELETE 文（または TRUNCATE）を発行します。
   */
  @Modifying
  @Query("DELETE FROM m_employee")
  void deleteAllFast();
}
