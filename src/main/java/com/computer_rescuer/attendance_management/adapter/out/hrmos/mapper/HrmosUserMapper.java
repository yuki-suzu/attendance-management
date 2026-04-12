package com.computer_rescuer.attendance_management.adapter.out.hrmos.mapper;

import com.computer_rescuer.attendance_management.adapter.out.hrmos.model.HrmosUser;
import com.computer_rescuer.attendance_management.domain.model.Employee;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * HRMOS固有のユーザーデータモデルと、システム共通の従業員ドメインモデルを相互変換するマッパーインターフェース。
 * <p>
 * MapStructライブラリにより、コンパイル時に本インターフェースの実装クラスが自動生成されます。 SpringのDIコンテナ（componentModel =
 * "spring"）で管理されるため、 他のコンポーネントから直接インジェクションして利用可能です。
 * </p>
 */
@Mapper(componentModel = "spring")
public interface HrmosUserMapper {

  /**
   * HRMOSのユーザーモデルを、従業員ドメインモデル(Employee)に変換します。
   * <p>
   * HRMOS側の項目名「number」を、ドメイン側の項目名「employeeNumber」へマッピングします。
   * その他の項目については、名称および型が一致しているため自動でマッピングされます。
   * </p>
   *
   * @param hrmosUser 変換元のHRMOSユーザーデータモデル
   * @return 変換後の従業員ドメインモデル
   */
  @Mapping(source = "number", target = "employeeNumber")
  Employee toDomain(HrmosUser hrmosUser);

  /**
   * HRMOSのユーザーモデルのリストを、従業員ドメインモデルのリストに一括変換します。
   * <p>
   * 内部的には {@link #toDomain(HrmosUser)} を繰り返し呼び出し、変換処理を行います。
   * </p>
   *
   * @param hrmosUsers 変換元のHRMOSユーザーデータモデルのリスト
   * @return 変換後の従業員ドメインモデルのリスト。入力がnullの場合はnullを返却します。
   */
  List<Employee> toDomainList(List<HrmosUser> hrmosUsers);

}
