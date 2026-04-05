package com.computer_rescuer.attendance_management.adapter.out.hrmos.client;

import com.computer_rescuer.attendance_management.adapter.out.hrmos.model.HrmosTokenResponse;
import com.computer_rescuer.attendance_management.adapter.out.hrmos.model.HrmosUser;
import com.computer_rescuer.attendance_management.infrastructure.property.HrmosProperties;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class HrmosApiClient {

  private final RestClient restClient;
  private final HrmosProperties properties;

  public HrmosApiClient(RestClient.Builder restClientBuilder, HrmosProperties properties) {
    this.properties = properties;
    this.restClient = restClientBuilder
        .baseUrl(properties.baseUrl())
        .build();
  }

  /**
   * HRMOSのSecret Keyを使って一時Tokenを取得する
   */
  public String fetchToken() {
    log.info("HRMOSから認証Tokenを取得します...");

    HrmosTokenResponse response = restClient.get()
        .uri("/authentication/token")
        // HRMOSの仕様: Secret KeyをBasic認証の「ユーザー名」として渡す（パスワードは空）
        .headers(headers -> headers.setBasicAuth(properties.secretKey(), ""))
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .body(HrmosTokenResponse.class);

    if (response == null || response.token() == null) {
      throw new IllegalStateException("HRMOSからのToken取得に失敗しました。レスポンスが空です。");
    }

    return response.token();
  }

  /**
   * 従業員マスタ（ユーザー一覧）を取得する
   *
   * @param token fetchToken() で取得した一時Token
   */
  public List<HrmosUser> fetchUsers(String token) {
    log.info("HRMOSからユーザー一覧を取得します...");

    // 仕様書より: 戻り値の型はJSONの配列（List<HrmosUser>）
    List<HrmosUser> users = restClient.get()
        // limit=100 (最大値) を指定。※100名を超える場合は後でPagination処理を追加します
        .uri(uriBuilder -> uriBuilder.path("/users").queryParam("limit", 100).build())
        .header(HttpHeaders.AUTHORIZATION, "Token " + token)
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .body(new ParameterizedTypeReference<>() {
        });

    if (users == null) {
      return List.of();
    }

    log.info("ユーザー情報を {} 件取得しました。", users.size());
    return users;
  }
}
