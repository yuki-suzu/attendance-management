package com.computer_rescuer.attendance_management.adapter.out.hrmos.client;

import com.computer_rescuer.attendance_management.adapter.out.exception.ExternalIntegrationException;
import com.computer_rescuer.attendance_management.adapter.out.hrmos.model.HrmosTokenResponse;
import com.computer_rescuer.attendance_management.adapter.out.hrmos.model.HrmosUser;
import com.computer_rescuer.attendance_management.infrastructure.property.HrmosProperties;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * HRMOS APIとのHTTP通信を担当するRESTクライアントクラス。
 * <p>
 * Spring Boot の {@link RestClient} を使用して、HRMOSに対する同期的なHTTPリクエストを実行します。
 * APIから4xxまたは5xxのエラーレスポンスが返却された場合は、自動的に {@link ExternalIntegrationException} をスローします。
 * </p>
 */
@Slf4j
@Component
public class HrmosApiClient {

  private final RestClient restClient;
  private final HrmosProperties properties;

  /**
   * プロパティ情報を元に、HRMOS向けのRestClientを構築します。
   *
   * @param restClientBuilder Spring Boot によって自動構成された RestClient.Builder
   * @param properties        application.yml に定義された HRMOS の設定値
   */
  public HrmosApiClient(RestClient.Builder restClientBuilder, HrmosProperties properties) {
    this.properties = properties;
    this.restClient = restClientBuilder.baseUrl(properties.baseUrl()).build();
  }

  /**
   * HRMOSの認証エンドポイントを呼び出し、API操作に必要な一時アクセストークンを取得します。
   * <p>
   * HRMOSの仕様に基づき、Secret KeyをBasic認証のユーザー名として送信します。
   * </p>
   *
   * @return 取得した一時アクセストークン文字列
   * @throws ExternalIntegrationException 通信エラー、認証失敗(401等)、またはレスポンスからトークンが取得できなかった場合
   */
  public String fetchToken() {
    log.info("HRMOSから認証Tokenを取得します...");

    HrmosTokenResponse response = restClient.get()
        .uri("/authentication/token")
        .headers(headers -> headers.setBasicAuth(properties.secretKey(), ""))
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .onStatus(HttpStatusCode::isError, (req, res) -> {
          String errorMsg = String.format("HRMOS Token取得APIでエラーが発生しました。Status: %s",
              res.getStatusCode());
          log.error(errorMsg);
          throw new ExternalIntegrationException(errorMsg);
        })
        .body(HrmosTokenResponse.class);

    if (response == null || response.token() == null) {
      throw new ExternalIntegrationException(
          "HRMOSからのToken取得に失敗しました。レスポンスが空です。");
    }

    return response.token();
  }

  /**
   * HRMOSの従業員（ユーザー）一覧エンドポイントを呼び出し、システム内の全従業員情報を取得します。
   *
   * @param token {@link #fetchToken()} で取得した有効な一時アクセストークン
   * @return 取得したHRMOSユーザーモデルのリスト。対象が存在しない場合は空のリストを返します。
   * @throws ExternalIntegrationException 通信エラー、またはHRMOS側でシステムエラー(5xx)等が発生した場合
   */
  public List<HrmosUser> fetchUsers(String token) {
    log.info("HRMOSからユーザー一覧を取得します...");

    List<HrmosUser> users = restClient.get()
        // ※現在は上限(limit=100)を指定しています。100名を超える場合はページネーション処理の追加が必要です。
        .uri(uriBuilder -> uriBuilder.path("/users").queryParam("limit", 100).build())
        .header(HttpHeaders.AUTHORIZATION, "Token " + token)
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .onStatus(HttpStatusCode::isError, (req, res) -> {
          String errorMsg = String.format("HRMOS ユーザー取得APIでエラーが発生しました。Status: %s",
              res.getStatusCode());
          log.error(errorMsg);
          throw new ExternalIntegrationException(errorMsg);
        })
        .body(new ParameterizedTypeReference<List<HrmosUser>>() {
        });

    if (users == null) {
      return List.of();
    }
    return users;
  }
}