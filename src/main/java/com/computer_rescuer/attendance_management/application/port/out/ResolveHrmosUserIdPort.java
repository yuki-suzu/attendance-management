package com.computer_rescuer.attendance_management.application.port.out;

import java.util.Optional;

/**
 * ドメインの従業員識別子（社員番号など）から、HRMOS固有のユーザーID（user_id）を解決する出力ポート。
 * <p>
 * アプリケーション層およびドメイン層は、このインターフェースを通じて 外部システムの都合である「内部ID（Integer）」を意識することなく、
 * 純粋な従業員識別子のみでビジネスロジックを構成できるようになります。
 * </p>
 */
public interface ResolveHrmosUserIdPort {

  /**
   * システム固有の従業員ID（社員番号など）に紐づく、HRMOSの user_id を取得します。
   *
   * @param employeeId システム固有の従業員識別子（例: 社員番号 "A0001"）
   * @return 紐づくHRMOSの user_id。存在しない場合は {@link Optional#empty()} を返却します。
   */
  Optional<Integer> resolve(String employeeId);
}
