package com.computer_rescuer.attendance_management.adapter.out.hrmos.client;

import com.computer_rescuer.attendance_management.adapter.out.exception.ExternalIntegrationException;
import com.computer_rescuer.attendance_management.adapter.out.hrmos.model.HrmosTokenResponse;
import com.computer_rescuer.attendance_management.infrastructure.property.HrmosProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * HRMOS APIとの低レベルなHTTP通信およびJSON解析をカプセル化するコアエンジン。
 * <p>
 * このクラスはパッケージプライベートとして定義され、外部（Adapter層など）からの直接利用を禁止します。
 * エラーハンドリング、ログ出力、JSONパースの複雑さを引き受け、各種公開APIクラスに共通の通信基盤を提供します。
 * </p>
 */
@Slf4j
@Component
class HrmosCoreHttpClient {

  private final RestClient restClient;
  private final HrmosProperties properties;
  private final ObjectMapper objectMapper;

  HrmosCoreHttpClient(RestClient.Builder restClientBuilder, HrmosProperties properties,
      ObjectMapper objectMapper) {
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.restClient = restClientBuilder
        .baseUrl(properties.baseUrl())
        .requestInterceptor((request, body, execution) -> {
          log.info("▶︎ [外部APIリクエスト] {} {}", request.getMethod(), request.getURI());
          if (log.isDebugEnabled()) {
            log.debug("▶︎ [ヘッダー] {}", request.getHeaders());
          }
          return execution.execute(request, body);
        }).build();
  }

  /**
   * HRMOSの認証エンドポイントから一時アクセストークンを取得します。
   * <p>
   * 設定ファイルに定義されたシークレットキーを用いてBasic認証を行い、 各種API操作に必須となるトークンを返却します。
   * </p>
   *
   * @return 取得したアクセストークン文字列
   * @throws ExternalIntegrationException 通信エラー、またはレスポンスからトークンが取得できなかった場合
   */
  String fetchToken() {
    log.info("HRMOSから認証Tokenを取得します...");
    HrmosTokenResponse response = restClient.get()
        .uri("/authentication/token")
        .header(HttpHeaders.AUTHORIZATION, "Basic " + properties.secretKey())
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .onStatus(HttpStatusCode::isError, (req, res) -> {
          throw new ExternalIntegrationException(
              "HRMOS Token取得APIエラー: " + res.getStatusCode());
        })
        .body(HrmosTokenResponse.class);

    if (response == null || response.token() == null) {
      throw new ExternalIntegrationException("HRMOSからのToken取得に失敗しました。");
    }
    return response.token();
  }

  /**
   * 指定されたエンドポイントからJSONリストを取得し、指定の型へ安全にデシリアライズします。
   * <p>
   * ページネーションを安全に処理するため、パスとパラメータを分離してURLを構築します。
   * </p>
   *
   * @param token         APIリクエストに必要なアクセストークン
   * @param path          呼び出し先のエンドポイントパス（例: "/users"）
   * @param page          取得対象のページ番号（1から開始）
   * @param jsonKey       JSONレスポンス内で目的の配列が格納されているキー名。ルート配列の場合は null。
   * @param resourceName  ログ出力に使用するリソースの論理名（例: "従業員"）
   * @param typeReference Jacksonでのデシリアライズに必要な型参照オブジェクト
   * @param <T>           返却されるリストの要素型
   * @return デシリアライズ済みのモデルリスト
   */
  <T> List<T> fetchAndParseList(String token, String path, int page, String jsonKey,
      String resourceName, TypeReference<List<T>> typeReference) {

    log.info("HRMOSから {} 一覧を取得します（page: {}）", resourceName, page);

    String rawJson = restClient.get()
        .uri(uriBuilder -> uriBuilder
            .path(path) // 👈 純粋なパスだけを渡す
            .queryParam("limit", 100)
            .queryParam("page", page)
            .build())
        .header(HttpHeaders.AUTHORIZATION, "Token " + token)
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .onStatus(HttpStatusCode::isError, (req, res) -> {
          throw new ExternalIntegrationException(
              String.format("HRMOS %s取得APIエラー。Status: %s", resourceName, res.getStatusCode()));
        })
        .body(String.class);

    if (log.isDebugEnabled()) {
      log.debug("◀︎ [HRMOS {} Raw JSON]:\n{}", path, rawJson);
    }

    if (rawJson == null || rawJson.isBlank()) {
      return List.of();
    }

    try {
      JsonNode root = objectMapper.readTree(rawJson);
      JsonNode dataNode = (jsonKey != null && root.has(jsonKey)) ? root.get(jsonKey) : root;
      return objectMapper.readValue(dataNode.traverse(), typeReference);
    } catch (Exception e) {
      log.error("{} のJSON解析に失敗しました。Raw JSON: {}", resourceName, rawJson, e);
      throw new ExternalIntegrationException(resourceName + "データ形式が予期せぬフォーマットです");
    }
  }
}
