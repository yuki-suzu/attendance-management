package com.computer_rescuer.attendance_management.adapter.out.hrmos.client;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.computer_rescuer.attendance_management.adapter.out.exception.ExternalIntegrationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link HrmosAuthApi} の単体テストクラス。
 * <p>
 * HRMOS 認証 API ファサードとして、コア HTTP クライアント（{@link HrmosCoreHttpClient}）への トークン取得委譲および例外伝播の命令網羅（C0
 * 100%）を検証します。 Spring コンテキストを起動せず、純粋な Mockito 単体テストとして高速に実行します。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HrmosAuthApi 単体テスト")
class HrmosAuthApiTest {

  @Mock
  private HrmosCoreHttpClient coreClient;

  @InjectMocks
  private HrmosAuthApi authApi;

  @Nested
  @DisplayName("認証トークン取得（fetchToken）シナリオ")
  class FetchTokenScenario {

    /**
     * コアクライアントから正常にトークンが返却された場合、そのままトークン文字列を返却することを検証します。
     */
    @Test
    @DisplayName("正常系: コアクライアントから取得したアクセストークンを返却すること")
    void shouldReturnTokenFromCoreClient() {
      // [Given]
      String expectedToken = "test-token-12345";
      given(coreClient.fetchToken()).willReturn(expectedToken);

      // [When]
      String actualToken = authApi.fetchToken();

      // [Then]
      assertAll(
          () -> assertEquals(expectedToken, actualToken, "トークン文字列が一致すること")
      );
      then(coreClient).should().fetchToken();
    }

    /**
     * コアクライアントで外部連携例外が発生した場合、例外を隠蔽せずそのままスローすることを検証します。
     */
    @Test
    @DisplayName("異常系: コアクライアントで認証失敗時に ExternalIntegrationException が伝播すること")
    void shouldPropagateExceptionWhenCoreClientFails() {
      // [Given]
      String errorMsg = "HRMOS Token取得APIエラー: 401 UNAUTHORIZED";
      given(coreClient.fetchToken()).willThrow(new ExternalIntegrationException(errorMsg));

      // [When & Then]
      ExternalIntegrationException exception = assertThrows(
          ExternalIntegrationException.class,
          () -> authApi.fetchToken(),
          "認証エラー例外がそのまま外へスローされること"
      );

      assertEquals(errorMsg, exception.getMessage());
      then(coreClient).should().fetchToken();
    }
  }
}
