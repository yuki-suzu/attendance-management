package com.computer_rescuer.attendance_management.adapter.out.hrmos.client;

import com.computer_rescuer.attendance_management.adapter.out.hrmos.model.HrmosUser;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 従業員（ユーザー）に関するHRMOS API操作を提供する公開クライアント。
 * <p>
 * アダプター層（Adapter）から直接呼び出される窓口であり、実際のHTTP通信処理は パッケージプライベートな {@link HrmosCoreHttpClient} へ委譲します。
 * </p>
 */
@Component
@RequiredArgsConstructor
public class HrmosUserApi {

  private final HrmosCoreHttpClient coreClient;

  /**
   * 従業員情報を取得するためのHRMOS一時アクセストークンを取得します。
   *
   * @return API通信用のアクセストークン文字列
   * @throws ExternalIntegrationException 認証に失敗した場合
   */
  public String fetchToken() {
    return coreClient.fetchToken();
  }

  /**
   * HRMOSからすべての従業員（ユーザー）情報を取得します。
   *
   * @param token API通信用の有効なアクセストークン
   * @return HRMOSユーザーモデルのリスト。対象が存在しない場合は空のリストを返却。
   * @throws ExternalIntegrationException 通信エラーやデータ解析エラーが発生した場合
   */
  public List<HrmosUser> fetchUsers(String token) {
    return coreClient.fetchAndParseList(
        token, "/users", "users", "従業員", new TypeReference<>() {
        }
    );
  }
}
