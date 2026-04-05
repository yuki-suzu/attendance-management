package com.computer_rescuer.attendance_management.adapter.out.hrmos.mapper;

import com.computer_rescuer.attendance_management.adapter.out.hrmos.model.HrmosUser;
import com.computer_rescuer.attendance_management.domain.model.Employee;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * HRMOSのデータモデルとシステム共通のドメインモデルを変換するマッパー。
 * <p>
 * MapStructにより、コンパイル時に実装クラスが自動生成されます。 componentModel = "spring"
 * を指定することで、SpringのDIコンテナにBeanとして登録されます。
 * </p>
 */
@Mapper(componentModel = "spring")
public interface HrmosUserMapper {

  /**
   * HRMOSのユーザーモデルをドメインモデル(Employee)に変換します。
   *
   * @param hrmosUser HRMOSのユーザーモデル
   * @return 従業員ドメインモデル
   */
  @Mapping(source = "number", target = "employeeNumber")
  Employee toDomain(HrmosUser hrmosUser);

}
