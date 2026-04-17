package com.computer_rescuer.attendance_management.adapter.out.persistence;

import com.computer_rescuer.attendance_management.adapter.out.persistence.jooq.EmployeeJooqRepository;
import com.computer_rescuer.attendance_management.adapter.out.persistence.mapper.EmployeeJooqMapper;
import com.computer_rescuer.attendance_management.application.port.out.EmployeeRepositoryPort;
import com.computer_rescuer.attendance_management.domain.model.Employee;
import com.computer_rescuer.attendance_management.generated.jooq.tables.records.MEmployeeRecord;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 従業員データの永続化に関する外部ポート（RepositoryPort）の実装アダプター。
 * <p>
 * アプリケーション層(Interactor)からの要求を受け付け、 内部のリポジトリやマッパーを組み合わせてデータベース操作を制御します。
 * アプリケーション層に対して、jOOQなどの具体的なデータアクセス技術を隠蔽します。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmployeePersistenceAdapter implements EmployeeRepositoryPort {

  private final EmployeeJooqRepository jooqRepository;
  private final EmployeeJooqMapper mapper;

  /**
   * 従業員マスタ(M_EMPLOYEE)の全データを削除します。
   */
  @Override
  public void deleteAllEmployees() {
    log.info("データベースの従業員マスタ(m_employee)を全件削除しています...");
    jooqRepository.deleteAll();
  }

  /**
   * ドメインモデルの従業員情報リストをデータベースに一括登録(Batch Insert)します。
   *
   * @param employees 登録対象となるドメインモデルの従業員リスト
   */
  @Override
  public void saveAll(List<Employee> employees) {
    log.info("従業員マスタに {} 件のデータを一括登録(Batch Insert)しています...", employees.size());

    // ドメインモデルをjOOQの永続化モデルに変換
    List<MEmployeeRecord> records = mapper.toRecords(employees);

    // jOOQリポジトリへ委譲
    jooqRepository.batchInsert(records);
  }
}
