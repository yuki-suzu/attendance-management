package com.computer_rescuer.attendance_management.application.port.out;

import com.computer_rescuer.attendance_management.domain.model.Department;
import com.computer_rescuer.attendance_management.domain.model.Segment;
import java.util.List;

/**
 * マスタデータの永続化を担う出力ポート。
 */
public interface MasterDataRepositoryPort {

  /**
   * 部門マスタを洗い替えます。 * @param departments 新しい部門リスト
   */
  void syncDepartments(List<Department> departments);

  /**
   * 勤務区分マスタを洗い替えます。 * @param segments 新しい勤務区分リスト
   */
  void syncSegments(List<Segment> segments);
}
