package com.computer_rescuer.attendance_management.adapter.out.persistence;

import com.computer_rescuer.attendance_management.adapter.out.persistence.entity.EmployeeEntity;
import com.computer_rescuer.attendance_management.adapter.out.persistence.jdbc.EmployeeJdbcRepository;
import com.computer_rescuer.attendance_management.adapter.out.persistence.mapper.EmployeePersistenceMapper;
import com.computer_rescuer.attendance_management.application.port.out.EmployeeRepositoryPort;
import com.computer_rescuer.attendance_management.domain.model.Employee;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 従業員情報の永続化を担当する出力アダプターの実装クラス。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmployeePersistenceAdapter implements EmployeeRepositoryPort {

  private final EmployeeJdbcRepository repository;
  private final EmployeePersistenceMapper mapper;

  @Override
  public void deleteAllEmployees() {
    log.info("従業員マスタ(m_employee)のデータを全件削除します...");
    repository.deleteAllFast();
  }

  @Override
  public void saveAll(List<Employee> employees) {
    log.info("従業員マスタに {} 件のデータを一括登録(Batch Insert)します...", employees.size());

    // 1. ドメインモデル -> DBエンティティに変換
    List<EmployeeEntity> entities = employees.stream()
        .map(mapper::toEntity)
        .toList();

    // 2. Spring Data JDBC の saveAll (自動的にJDBCバッチインサートが実行されます)
    repository.saveAll(entities);
  }
}
