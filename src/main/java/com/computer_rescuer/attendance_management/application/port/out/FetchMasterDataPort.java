package com.computer_rescuer.attendance_management.application.port.out;

import com.computer_rescuer.attendance_management.domain.model.Department;
import com.computer_rescuer.attendance_management.domain.model.Segment;
import java.util.List;

/**
 * 外部システム（HRMOS等）からマスタデータを取得するための出力ポート。
 * <p>
 * アプリケーション層が外部APIの具体的な通信方式（HTTPクライアント、エンドポイント等） に依存しないよう、ドメインモデルでの取得インターフェースを提供します。
 * </p>
 */
public interface FetchMasterDataPort {

  /**
   * 外部システムからすべての部門情報を取得します。
   *
   * @return 部門ドメインモデルのリスト。取得できない場合は空のリスト。
   */
  List<Department> fetchAllDepartments();

  /**
   * 外部システムからすべての勤務区分情報を取得します。
   *
   * @return 勤務区分ドメインモデルのリスト。取得できない場合は空のリスト。
   */
  List<Segment> fetchAllSegments();
}
