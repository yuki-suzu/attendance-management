package com.computer_rescuer.attendance_management.adapter.out.hrmos;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.computer_rescuer.attendance_management.adapter.out.hrmos.client.HrmosAuthApi;
import com.computer_rescuer.attendance_management.adapter.out.hrmos.client.HrmosDepartmentApi;
import com.computer_rescuer.attendance_management.adapter.out.hrmos.client.HrmosSegmentApi;
import com.computer_rescuer.attendance_management.adapter.out.hrmos.mapper.HrmosDepartmentMapper;
import com.computer_rescuer.attendance_management.adapter.out.hrmos.mapper.HrmosSegmentMapper;
import com.computer_rescuer.attendance_management.adapter.out.hrmos.model.HrmosDepartment;
import com.computer_rescuer.attendance_management.adapter.out.hrmos.model.HrmosSegment;
import com.computer_rescuer.attendance_management.domain.model.Department;
import com.computer_rescuer.attendance_management.domain.model.Segment;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link HrmosMasterDataAdapter} の単体テストクラス。
 * <p>
 * マスタデータ取得出力ポートの実装として、部門マスタおよび勤務区分マスタの API 呼び出しとドメインモデル変換連携における命令網羅（C0 100%）を検証します。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HrmosMasterDataAdapter 単体テスト")
class HrmosMasterDataAdapterTest {

  @Mock
  private HrmosAuthApi authApi;

  @Mock
  private HrmosDepartmentApi departmentApi;

  @Mock
  private HrmosSegmentApi segmentApi;

  @Mock
  private HrmosDepartmentMapper departmentMapper;

  @Mock
  private HrmosSegmentMapper segmentMapper;

  @InjectMocks
  private HrmosMasterDataAdapter adapter;

  private final String token = "test-auth-token";

  @Nested
  @DisplayName("部門マスタ全件取得（fetchAllDepartments）シナリオ")
  class FetchAllDepartmentsScenario {

    @Test
    @DisplayName("正常系: 部門 API からデータを取得し、部門ドメインモデルのリストとして返却すること")
    void shouldFetchAllDepartmentsSuccessfully() {
      // [Given]
      given(authApi.fetchToken()).willReturn(token);

      HrmosDepartment rawDept = mock(HrmosDepartment.class);
      List<HrmosDepartment> rawList = List.of(rawDept);
      given(departmentApi.fetchDepartments(token)).willReturn(rawList);

      Department domainDept = mock(Department.class);
      List<Department> expectedDepts = List.of(domainDept);
      given(departmentMapper.toDomainList(rawList)).willReturn(expectedDepts);

      // [When]
      List<Department> actualDepts = adapter.fetchAllDepartments();

      // [Then]
      assertAll(
          () -> assertNotNull(actualDepts),
          () -> assertEquals(1, actualDepts.size()),
          () -> assertEquals(expectedDepts, actualDepts)
      );
      then(authApi).should().fetchToken();
      then(departmentApi).should().fetchDepartments(token);
      then(departmentMapper).should().toDomainList(rawList);
    }
  }

  @Nested
  @DisplayName("勤務区分マスタ全件取得（fetchAllSegments）シナリオ")
  class FetchAllSegmentsScenario {

    @Test
    @DisplayName("正常系: 勤務区分 API からデータを取得し、勤務区分ドメインモデルのリストとして返却すること")
    void shouldFetchAllSegmentsSuccessfully() {
      // [Given]
      given(authApi.fetchToken()).willReturn(token);

      HrmosSegment rawSegment = mock(HrmosSegment.class);
      List<HrmosSegment> rawList = List.of(rawSegment);
      given(segmentApi.fetchSegments(token)).willReturn(rawList);

      Segment domainSegment = mock(Segment.class);
      List<Segment> expectedSegments = List.of(domainSegment);
      given(segmentMapper.toDomainList(rawList)).willReturn(expectedSegments);

      // [When]
      List<Segment> actualSegments = adapter.fetchAllSegments();

      // [Then]
      assertAll(
          () -> assertNotNull(actualSegments),
          () -> assertEquals(1, actualSegments.size()),
          () -> assertEquals(expectedSegments, actualSegments)
      );
      then(authApi).should().fetchToken();
      then(segmentApi).should().fetchSegments(token);
      then(segmentMapper).should().toDomainList(rawList);
    }
  }
}
