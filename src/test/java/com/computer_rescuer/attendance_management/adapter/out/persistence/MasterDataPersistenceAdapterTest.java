package com.computer_rescuer.attendance_management.adapter.out.persistence;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.computer_rescuer.attendance_management.adapter.out.persistence.jooq.DepartmentJooqRepository;
import com.computer_rescuer.attendance_management.adapter.out.persistence.jooq.SegmentJooqRepository;
import com.computer_rescuer.attendance_management.adapter.out.persistence.mapper.DepartmentJooqMapper;
import com.computer_rescuer.attendance_management.adapter.out.persistence.mapper.SegmentJooqMapper;
import com.computer_rescuer.attendance_management.domain.model.Department;
import com.computer_rescuer.attendance_management.domain.model.Segment;
import com.computer_rescuer.attendance_management.generated.jooq.tables.records.MDepartmentRecord;
import com.computer_rescuer.attendance_management.generated.jooq.tables.records.MSegmentRecord;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link MasterDataPersistenceAdapter} の単体テストクラス。
 * <p>
 * 部門マスタおよび勤務区分マスタ同期ポート（{@link
 * com.computer_rescuer.attendance_management.application.port.out.MasterDataRepositoryPort}）の実装として、
 * 洗い替え処理（既存レコード全削除 → 新規一括登録）の実行順序およびマッピング連携を検証します。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MasterDataPersistenceAdapter 単体テスト")
class MasterDataPersistenceAdapterTest {

  @Mock
  private DepartmentJooqRepository departmentRepository;

  @Mock
  private SegmentJooqRepository segmentRepository;

  @Mock
  private DepartmentJooqMapper departmentMapper;

  @Mock
  private SegmentJooqMapper segmentMapper;

  @InjectMocks
  private MasterDataPersistenceAdapter adapter;

  @Nested
  @DisplayName("部門マスタ同期（syncDepartments）シナリオ")
  class SyncDepartmentsScenario {

    /**
     * 【テスト観点 / 責務】 部門マスタ同期時、既存レコードの全削除後に新規レコードの一括登録が順序正しく実行されることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>部門ドメインモデルのリスト</li>
     *   <li>マッパーが変換した {@link MDepartmentRecord} のリストを用意</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>{@link DepartmentJooqRepository#deleteAll()} が最初に実行されること</li>
     *   <li>{@link DepartmentJooqRepository#batchInsert(List)} が次に実行されること</li>
     * </ul>
     */
    @Test
    @DisplayName("正常系: 部門マスタ同期時、deleteAll後にbatchInsertが順次呼び出されること")
    void shouldDeleteAllAndBatchInsertDepartmentsInOrder() {
      // [Given]
      Department department = mock(Department.class);
      List<Department> departments = List.of(department);

      MDepartmentRecord record = mock(MDepartmentRecord.class);
      List<MDepartmentRecord> records = List.of(record);

      when(departmentMapper.toRecords(departments)).thenReturn(records);

      // [When]
      adapter.syncDepartments(departments);

      // [Then]
      InOrder inOrder = inOrder(departmentRepository);
      inOrder.verify(departmentRepository).deleteAll();
      inOrder.verify(departmentRepository).batchInsert(records);
    }
  }

  @Nested
  @DisplayName("勤務区分マスタ同期（syncSegments）シナリオ")
  class SyncSegmentsScenario {

    /**
     * 【テスト観点 / 責務】 勤務区分マスタ同期時、既存レコードの全削除後に新規レコードの一括登録が順序正しく実行されることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>勤務区分ドメインモデルのリスト</li>
     *   <li>マッパーが変換した {@link MSegmentRecord} のリストを用意</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>{@link SegmentJooqRepository#deleteAll()} が最初に実行されること</li>
     *   <li>{@link SegmentJooqRepository#batchInsert(List)} が次に実行されること</li>
     * </ul>
     */
    @Test
    @DisplayName("正常系: 勤務区分マスタ同期時、deleteAll後にbatchInsertが順次呼び出されること")
    void shouldDeleteAllAndBatchInsertSegmentsInOrder() {
      // [Given]
      Segment segment = mock(Segment.class);
      List<Segment> segments = List.of(segment);

      MSegmentRecord record = mock(MSegmentRecord.class);
      List<MSegmentRecord> records = List.of(record);

      when(segmentMapper.toRecords(segments)).thenReturn(records);

      // [When]
      adapter.syncSegments(segments);

      // [Then]
      InOrder inOrder = inOrder(segmentRepository);
      inOrder.verify(segmentRepository).deleteAll();
      inOrder.verify(segmentRepository).batchInsert(records);
    }
  }
}
