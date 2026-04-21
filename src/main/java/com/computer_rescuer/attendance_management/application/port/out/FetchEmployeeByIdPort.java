package com.computer_rescuer.attendance_management.application.port.out;

import com.computer_rescuer.attendance_management.domain.model.Employee;
import java.util.List;
import java.util.Map;

/**
 * ユーザーIDのリストから、ローカルデータベースの従業員情報（Employee）を一括取得する出力ポート。
 * <p>
 * N+1問題を回避するため、必ずリストを引数に取り Map で返却するバルク取得専用のインターフェースです。
 * </p>
 */
public interface FetchEmployeeByIdPort {

  /**
   * 指定されたユーザーIDのリストに紐づく従業員情報を取得します。
   *
   * @param userIds 取得対象のユーザーID（m_employee.id）のリスト
   * @return ユーザーIDをキーとした、従業員情報(Employee)のMap。該当がないIDのキーは含まれません。
   */
  Map<Integer, Employee> fetchEmployeeMapByUserIds(List<Integer> userIds);
}
