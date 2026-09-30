package com.computer_rescuer.attendance_management.adapter.out.hrmos.client;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.computer_rescuer.attendance_management.adapter.out.hrmos.model.HrmosUser;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.core.type.TypeReference;

/**
 * {@link HrmosUserApi} の単体テストクラス。
 * <p>
 * HRMOS ユーザー（従業員マスタ）API ファサードとして、認証トークン取得委譲、 および従業員一覧エンドポイント（"/users"）へのページング要求委譲の命令網羅（C0
 * 100%）を検証します。 Spring コンテキストを起動せず、純粋な Mockito 単体テストとして高速に実行します。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HrmosUserApi 単体テスト")
class HrmosUserApiTest {

  @Mock
  private HrmosCoreHttpClient coreClient;

  @InjectMocks
  private HrmosUserApi userApi;

  @Nested
  @DisplayName("認証トークン取得（fetchToken）シナリオ")
  class FetchTokenScenario {

    /**
     * コアクライアントの fetchToken が呼び出され、返却されたトークン文字列がそのまま返却されることを検証します。
     */
    @Test
    @DisplayName("正常系: コアクライアントから取得したアクセストークンを返却すること")
    void shouldReturnTokenFromCoreClient() {
      // [Given]
      String expectedToken = "user-api-token-abc";
      given(coreClient.fetchToken()).willReturn(expectedToken);

      // [When]
      String actualToken = userApi.fetchToken();

      // [Then]
      assertEquals(expectedToken, actualToken, "トークン文字列が一致すること");
      then(coreClient).should().fetchToken();
    }
  }

  @Nested
  @DisplayName("従業員一覧取得（fetchUsers）シナリオ")
  class FetchUsersScenario {

    /**
     * 指定されたページ番号を用いて "/users" エンドポイントへリクエストが委譲され、 従業員生データリストが返却されることを検証します。
     */
    @Test
    @DisplayName("正常系: /users エンドポイントに対してコアクライアントを実行し、従業員リストを返却すること")
    void shouldFetchUsersThroughCoreClient() {
      // [Given]
      String token = "dummy-token";
      int page = 2;
      HrmosUser mockUser = mock(HrmosUser.class);
      List<HrmosUser> expectedUsers = List.of(mockUser);

      given(coreClient.fetchAndParseList(
          eq(token),
          eq("/users"),
          eq(page),
          eq("従業員"),
          any(TypeReference.class)
      )).willReturn(expectedUsers);

      // [When]
      List<HrmosUser> actualUsers = userApi.fetchUsers(token, page);

      // [Then]
      assertAll(
          () -> assertNotNull(actualUsers, "返却リストが null でないこと"),
          () -> assertEquals(1, actualUsers.size(), "要素数が一致すること"),
          () -> assertEquals(expectedUsers, actualUsers, "取得結果が一致すること")
      );
      then(coreClient).should().fetchAndParseList(
          eq(token),
          eq("/users"),
          eq(page),
          eq("従業員"),
          any(TypeReference.class)
      );
    }
  }
}
