package com.computer_rescuer.attendance_management.application.port.out;

import java.util.List;
import java.util.Map;

/**
 * 従業員の所属部門を取得するための出力ポート。
 * <p>
 * Application層は、このインターフェースを通じて従業員の最新の所属情報を要求します。<br> HRMOS
 * APIの不完全な文字列に依存せず、ローカルデータベースのマスタ情報を正とするための要となります。
 * </p>
 */
public interface FetchEmployeeDepartmentPort {

  /**
   * 指定されたユーザーIDのリストを元に、ユーザーIDと部門名のマップを取得します。
   *
   * @param userIds 対象となる従業員の内部IDリスト
   * @return ユーザーIDをキー、部門名を値とするマップ（所属が存在しない場合は "未所属" などの代替文字列が含まれる想定）
   */
  Map<Integer, String> fetchDepartmentMapByUserIds(List<Integer> userIds);
}
