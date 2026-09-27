package com.computer_rescuer.attendance_management.adapter.out.hrmos.client;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.computer_rescuer.attendance_management.adapter.out.exception.ExternalIntegrationException;
import com.computer_rescuer.attendance_management.adapter.out.hrmos.model.HrmosDepartment;
import com.computer_rescuer.attendance_management.infrastructure.property.HrmosProperties;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * {@link HrmosCoreHttpClient} の単体テストクラス。
 * <p>
 * {@link MockRestServiceServer} をバインドして HTTP 通信を完全にインメモリでシミュレートし、 認証ヘッダー付与、URL
 * クエリパラメータ構築、インターセプターログ実行、ステータスコードエラー判定、 および Jackson デシリアライズ例外・早期リターンの命令網羅（C0 100%）を検証します。 Spring
 * コンテキストを起動せず、純粋な単体テストとして高速に実行します。
 * </p>
 */
@DisplayName("HrmosCoreHttpClient 単体テスト")
class HrmosCoreHttpClientTest {

  private MockRestServiceServer mockServer;
  private HrmosCoreHttpClient coreClient;

  private final String baseUrl = "https://ieyasu.co/api/test-corp/v1";
  private final String secretKey = "test-secret-key";
  private final HrmosProperties properties = new HrmosProperties("test-corp", secretKey, baseUrl);
  private final JsonMapper jsonMapper = JsonMapper.builder().build();

  @BeforeEach
  void setUp() {
    RestClient.Builder restClientBuilder = RestClient.builder();
    // モックサーバーを Builder に直接バインドして本物の RestClient の通信を差し押さえる
    mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
    coreClient = new HrmosCoreHttpClient(restClientBuilder, properties, jsonMapper);
  }

  @Nested
  @DisplayName("認証トークン取得（fetchToken）シナリオ")
  class FetchTokenScenario {

    @Test
    @DisplayName("正常系: 認証 API に Basic 認証ヘッダーでリクエストし、トークン文字列を取得できること")
    void shouldFetchTokenSuccessfully() {
      // [Given]
      String responseJson = """
          {
            "token": "generated-token-xyz",
            "expired_at": "2026-04-02T10:00:00.000+09:00"
          }
          """;

      mockServer.expect(requestTo(baseUrl + "/authentication/token"))
          .andExpect(method(HttpMethod.GET))
          .andExpect(header(HttpHeaders.AUTHORIZATION, "Basic " + secretKey))
          .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
          .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

      // [When]
      String token = coreClient.fetchToken();

      // [Then]
      assertAll(
          () -> assertEquals("generated-token-xyz", token, "取得したトークンが一致すること")
      );
      mockServer.verify();
    }

    @Test
    @DisplayName("異常系: 認証 API が 401 Unauthorized を返却した場合、ExternalIntegrationException をスローすること")
    void shouldThrowExceptionWhenTokenApiReturnsErrorStatus() {
      // [Given]
      mockServer.expect(requestTo(baseUrl + "/authentication/token"))
          .andExpect(method(HttpMethod.GET))
          .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

      // [When & Then]
      ExternalIntegrationException exception = assertThrows(
          ExternalIntegrationException.class,
          () -> coreClient.fetchToken(),
          "HTTP エラー発生時は外部連携例外がスローされること"
      );

      assertTrue(exception.getMessage().contains("HRMOS Token取得APIエラー: 401 UNAUTHORIZED"));
      mockServer.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{}",                                 // token フィールド欠落
        "{\"token\": null}",                  // token が明示的 null
        "{\"expired_at\": \"2026-04-02\"}"    // token が存在しない
    })
    @DisplayName("異常系: レスポンスボディ内のトークンが null の場合、ExternalIntegrationException をスローすること")
    void shouldThrowExceptionWhenTokenIsNullInResponse(String responseJson) {
      // [Given]
      mockServer.expect(requestTo(baseUrl + "/authentication/token"))
          .andExpect(method(HttpMethod.GET))
          .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

      // [When & Then]
      ExternalIntegrationException exception = assertThrows(
          ExternalIntegrationException.class,
          () -> coreClient.fetchToken(),
          "Token が取得できない場合は例外がスローされること"
      );

      assertEquals("HRMOSからのToken取得に失敗しました。", exception.getMessage());
      mockServer.verify();
    }
  }

  @Nested
  @DisplayName("リストデータ取得および解析（fetchAndParseList）シナリオ")
  class FetchAndParseListScenario {

    private final TypeReference<List<HrmosDepartment>> departmentTypeRef = new TypeReference<>() {
    };

    @Test
    @DisplayName("正常系: 任意のクエリパラメータなしのオーバーロードから正しくリクエストが送信され解析されること")
    void shouldFetchAndParseListWithoutExtraQueryParams() {
      // [Given]
      String token = "valid-token";
      String responseJson = """
          [
            { "id": 1, "name": "総務部", "sequence": 1 },
            { "id": 2, "name": "人事部", "sequence": 2 }
          ]
          """;

      // limit=100 と page=1 がデフォルト付与されることを検証
      mockServer.expect(requestTo(baseUrl + "/departments?limit=100&page=1"))
          .andExpect(method(HttpMethod.GET))
          .andExpect(header(HttpHeaders.AUTHORIZATION, "Token " + token))
          .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
          .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

      // [When]
      List<HrmosDepartment> actualList = coreClient.fetchAndParseList(
          token, "/departments", 1, "部門", departmentTypeRef);

      // [Then]
      assertAll(
          () -> assertNotNull(actualList),
          () -> assertEquals(2, actualList.size(), "2件取得できること"),
          () -> assertEquals("総務部", actualList.getFirst().name()),
          () -> assertEquals("人事部", actualList.get(1).name())
      );
      mockServer.verify();
    }

    @Test
    @DisplayName("正常系: 追加クエリパラメータ（from, to）が存在する場合、URL に安全に追加エンコードされること")
    void shouldFetchAndParseListWithExtraQueryParams() {
      // [Given]
      String token = "valid-token";
      Map<String, String> extraParams = Map.of(
          "from", "2026-04-01T00:00:00+09:00",
          "to", "2026-04-30T23:59:59+09:00"
      );
      String responseJson = "[{ \"id\": 10, \"name\": \"開発部\", \"sequence\": 1 }]";

      mockServer.expect(requestTo(org.hamcrest.Matchers.allOf(
              org.hamcrest.Matchers.startsWith(baseUrl + "/departments?"),
              org.hamcrest.Matchers.containsString("limit=100"),
              org.hamcrest.Matchers.containsString("page=2"),
              org.hamcrest.Matchers.containsString("from=2026-04-01T00:00:00+09:00"),
              org.hamcrest.Matchers.containsString("to=2026-04-30T23:59:59+09:00")
          )))
          .andExpect(method(HttpMethod.GET))
          .andExpect(header(HttpHeaders.AUTHORIZATION, "Token " + token))
          .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

      // [When]
      List<HrmosDepartment> actualList = coreClient.fetchAndParseList(
          token, "/departments", 2, extraParams, "部門", departmentTypeRef);

      // [Then]
      assertEquals(1, actualList.size());
      assertEquals("開発部", actualList.getFirst().name());
      mockServer.verify();
    }

    @Test
    @DisplayName("正常系: 追加クエリパラメータが空マップの場合でも正常にリクエストが構築されること")
    void shouldHandleEmptyExtraQueryParams() {
      // [Given]
      String token = "valid-token";
      mockServer.expect(requestTo(baseUrl + "/departments?limit=100&page=1"))
          .andExpect(method(HttpMethod.GET))
          .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

      // [When]
      List<HrmosDepartment> actualList = coreClient.fetchAndParseList(
          token, "/departments", 1, Collections.emptyMap(), "部門", departmentTypeRef);

      // [Then]
      assertTrue(actualList.isEmpty(), "空リストが返ること");
      mockServer.verify();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("正常系: レスポンスが空または空白文字の場合、空リストを早期リターンすること")
    void shouldReturnEmptyListWhenRawJsonIsBlank(String blankResponse) {
      // [Given]
      String token = "valid-token";
      mockServer.expect(requestTo(baseUrl + "/departments?limit=100&page=1"))
          .andExpect(method(HttpMethod.GET))
          .andRespond(
              withSuccess(blankResponse != null ? blankResponse : "", MediaType.APPLICATION_JSON));

      // [When]
      List<HrmosDepartment> actualList = coreClient.fetchAndParseList(
          token, "/departments", 1, null, "部門", departmentTypeRef);

      // [Then]
      assertNotNull(actualList);
      assertTrue(actualList.isEmpty(), "空リストが返ること");
      mockServer.verify();
    }

    @Test
    @DisplayName("異常系: API が 500 Internal Server Error を返却した場合、ExternalIntegrationException をスローすること")
    void shouldThrowExceptionWhenApiReturnsServerError() {
      // [Given]
      String token = "valid-token";
      mockServer.expect(requestTo(baseUrl + "/departments?limit=100&page=1"))
          .andExpect(method(HttpMethod.GET))
          .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

      // [When & Then]
      ExternalIntegrationException exception = assertThrows(
          ExternalIntegrationException.class,
          () -> coreClient.fetchAndParseList(token, "/departments", 1, "部門", departmentTypeRef),
          "ステータスエラー時は例外がスローされること"
      );

      assertTrue(exception.getMessage()
          .contains("HRMOS 部門取得APIエラー。Status: 500 INTERNAL_SERVER_ERROR"));
      mockServer.verify();
    }

    @Test
    @DisplayName("異常系: JSON 解析に失敗する不正なフォーマットの場合、ExternalIntegrationException をスローすること")
    void shouldThrowExceptionWhenJsonParsingFails() {
      // [Given]
      String token = "valid-token";
      String invalidJson = "{ invalid-json-syntax }";

      mockServer.expect(requestTo(baseUrl + "/departments?limit=100&page=1"))
          .andExpect(method(HttpMethod.GET))
          .andRespond(withSuccess(invalidJson, MediaType.APPLICATION_JSON));

      // [When & Then]
      ExternalIntegrationException exception = assertThrows(
          ExternalIntegrationException.class,
          () -> coreClient.fetchAndParseList(token, "/departments", 1, "部門", departmentTypeRef),
          "JSON パースエラー時は外部連携例外にラップされること"
      );

      assertEquals("部門データ形式が予期せぬフォーマットです", exception.getMessage());
      mockServer.verify();
    }
  }
}
