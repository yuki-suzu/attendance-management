package com.computer_rescuer.attendance_management.adapter.out.persistence;

import com.computer_rescuer.attendance_management.adapter.out.persistence.jooq.DepartmentJooqRepository;
import com.computer_rescuer.attendance_management.adapter.out.persistence.jooq.SegmentJooqRepository;
import com.computer_rescuer.attendance_management.adapter.out.persistence.mapper.DepartmentJooqMapper;
import com.computer_rescuer.attendance_management.adapter.out.persistence.mapper.SegmentJooqMapper;
import com.computer_rescuer.attendance_management.application.port.out.MasterDataRepositoryPort;
import com.computer_rescuer.attendance_management.domain.model.Department;
import com.computer_rescuer.attendance_management.domain.model.Segment;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 各種マスタデータの永続化を制御する出力アダプター。
 * <p>
 * 個別のテーブルに対応するRepositoryとMapperを組み合わせ、 ドメイン層の要求するマスタ同期処理を具体化します。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MasterDataPersistenceAdapter implements MasterDataRepositoryPort {

  private final DepartmentJooqRepository departmentRepository;
  private final SegmentJooqRepository segmentRepository;
  private final DepartmentJooqMapper departmentMapper;
  private final SegmentJooqMapper segmentMapper;

  @Override
  public void syncDepartments(List<Department> departments) {
    log.info("部門マスタの同期処理を開始します（件数: {}）", departments.size());
    departmentRepository.deleteAll();
    departmentRepository.batchInsert(departmentMapper.toRecords(departments));
  }

  @Override
  public void syncSegments(List<Segment> segments) {
    log.info("勤務区分マスタの同期処理を開始します（件数: {}）", segments.size());
    segmentRepository.deleteAll();
    segmentRepository.batchInsert(segmentMapper.toRecords(segments));
  }
}
