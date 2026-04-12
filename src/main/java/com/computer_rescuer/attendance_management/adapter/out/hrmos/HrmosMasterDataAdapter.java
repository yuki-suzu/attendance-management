package com.computer_rescuer.attendance_management.adapter.out.hrmos;

import com.computer_rescuer.attendance_management.adapter.out.hrmos.client.HrmosAuthApi;
import com.computer_rescuer.attendance_management.adapter.out.hrmos.client.HrmosDepartmentApi;
import com.computer_rescuer.attendance_management.adapter.out.hrmos.client.HrmosSegmentApi;
import com.computer_rescuer.attendance_management.adapter.out.hrmos.mapper.HrmosDepartmentMapper;
import com.computer_rescuer.attendance_management.adapter.out.hrmos.mapper.HrmosSegmentMapper;
import com.computer_rescuer.attendance_management.application.port.out.FetchMasterDataPort;
import com.computer_rescuer.attendance_management.domain.model.Department;
import com.computer_rescuer.attendance_management.domain.model.Segment;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * HRMOS APIを使用して各種マスタデータを取得する出力アダプター。
 * <p>
 * リソースごとに分割されたAPIクライアントとマッパーをオーケストレーションし、 アプリケーション層へ純粋なドメインモデルを提供します。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HrmosMasterDataAdapter implements FetchMasterDataPort {

  private final HrmosAuthApi authApi;
  private final HrmosDepartmentApi departmentApi;
  private final HrmosSegmentApi segmentApi;

  // 詰め合わせマッパーを廃止し、専用マッパーを利用
  private final HrmosDepartmentMapper departmentMapper;
  private final HrmosSegmentMapper segmentMapper;

  @Override
  public List<Department> fetchAllDepartments() {
    String token = authApi.fetchToken();
    return departmentMapper.toDomainList(departmentApi.fetchDepartments(token));
  }

  @Override
  public List<Segment> fetchAllSegments() {
    String token = authApi.fetchToken();
    return segmentMapper.toDomainList(segmentApi.fetchSegments(token));
  }
}
