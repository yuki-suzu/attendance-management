package com.computer_rescuer.attendance_management.adapter.out.hrmos.mapper;

import com.computer_rescuer.attendance_management.adapter.out.hrmos.model.HrmosDepartment;
import com.computer_rescuer.attendance_management.domain.model.Department;
import java.util.List;
import org.mapstruct.Mapper;

/**
 * HRMOS部門モデルとドメイン部門モデルを相互変換するマッパー。
 */
@Mapper(componentModel = "spring")
public interface HrmosDepartmentMapper {

  /**
   * HRMOSの部門モデルをドメインモデルに変換します。
   *
   * @param hrmosDepartment 変換元のHRMOS部門モデル
   * @return 変換後の部門ドメインモデル
   */
  Department toDomain(HrmosDepartment hrmosDepartment);

  /**
   * HRMOSの部門モデルリストをドメインモデルリストに一括変換します。
   *
   * @param hrmosDepartments 変換元のリスト
   * @return 変換後のリスト
   */
  List<Department> toDomainList(List<HrmosDepartment> hrmosDepartments);
}
