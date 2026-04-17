package com.computer_rescuer.attendance_management.adapter.out.persistence.mapper;

import com.computer_rescuer.attendance_management.adapter.out.persistence.entity.EmployeeEntity;
import com.computer_rescuer.attendance_management.domain.model.Employee;
import org.mapstruct.Mapper;

/**
 * ドメインモデルと永続化エンティティを変換するマッパー。
 */
@Mapper(componentModel = "spring")
public interface EmployeePersistenceMapper {

  // ドメインモデル -> DBエンティティへの変換
  EmployeeEntity toEntity(Employee domain);

  // DBエンティティ -> ドメインモデルへの変換（今回は参照系はjOOQを使う予定ですが、念のため用意）
  Employee toDomain(EmployeeEntity entity);
}
