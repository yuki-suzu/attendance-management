package com.computer_rescuer.attendance_management.adapter.out.hrmos.client;

import com.computer_rescuer.attendance_management.adapter.out.hrmos.model.HrmosUser;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HrmosUserApi {

  private final HrmosCoreHttpClient coreClient;

  public String fetchToken() {
    return coreClient.fetchToken();
  }

  /**
   * 指定したページの従業員（ユーザー）情報を取得します。
   *
   * @param token API通信用の有効なアクセストークン
   * @param page  取得対象のページ番号（1から開始）
   * @return 1ページ分のHRMOSユーザーモデルリスト
   */
  public List<HrmosUser> fetchUsers(String token, int page) {
    return coreClient.fetchAndParseList(
        token, "/users", page, "users", "従業員", new TypeReference<>() {
        }
    );
  }
}
