package com.computer_rescuer.attendance_management.adapter.out.hrmos.client;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.computer_rescuer.attendance_management.adapter.out.hrmos.model.HrmosStampLog;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.core.type.TypeReference;

/**
 * {@link HrmosStampLogApi} の単体テストクラス。
 * <p>
 * HRMOS 打刻履歴 API ファサードとして、全従業員日次打刻エンドポイントのパス生成、 およびユーザー別打刻履歴における期間指定パラメータ（from, to）のマップ構築分岐と コア
 * HTTP クライアント（{@link HrmosCoreHttpClient}）連携の命令網羅（C0 100%）を検証します。 Spring コンテキストを起動せず、純粋な Mockito
 * 単体テストとして高速に実行します。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HrmosStampLogApi 単体テスト")
class HrmosStampLogApiTest {

  @Mock
  private HrmosCoreHttpClient coreClient;

  @InjectMocks
  private HrmosStampLogApi stampLogApi;

  private final String token = "dummy-auth-token";

  @Nested
  @DisplayName("全従業員日次打刻ログ取得（fetchDailyStampLogs）シナリオ")
  class FetchDailyStampLogsScenario {

    /**
     * 指定された日付およびページ番号に基づいて正しいエンドポイントパスが構築され、 コアクライアントから取得された打刻ログ生データリストが返却されることを検証します。
     */
    @Test
    @DisplayName("正常系: 対象日付を含むパス（/stamp_logs/daily/{date}）でコアクライアントを実行し、リストを返却すること")
    void shouldFetchDailyStampLogsThroughCoreClient() {
      // [Given]
      String targetDate = "2026-04-20";
      int page = 1;
      String expectedPath = "/stamp_logs/daily/2026-04-20";

      HrmosStampLog mockLog = mock(HrmosStampLog.class);
      List<HrmosStampLog> expectedLogs = List.of(mockLog);

      given(coreClient.fetchAndParseList(
          eq(token),
          eq(expectedPath),
          eq(page),
          eq("日次打刻ログ"),
          any(TypeReference.class)
      )).willReturn(expectedLogs);

      // [When]
      List<HrmosStampLog> actualLogs = stampLogApi.fetchDailyStampLogs(token, targetDate, page);

      // [Then]
      assertAll(
          () -> assertNotNull(actualLogs, "返却リストが null でないこと"),
          () -> assertEquals(1, actualLogs.size(), "件数が一致すること"),
          () -> assertEquals(expectedLogs, actualLogs, "取得結果が一致すること")
      );
      then(coreClient).should().fetchAndParseList(
          eq(token),
          eq(expectedPath),
          eq(page),
          eq("日次打刻ログ"),
          any(TypeReference.class)
      );
    }
  }

  @Nested
  @DisplayName("ユーザー別打刻ログ取得（fetchUserStampLogs）シナリオ")
  class FetchUserStampLogsScenario {

    private final Integer userId = 101;
    private final int page = 1;
    private final String expectedPath = "/stamp_logs/user/101";

    /**
     * from と to の両パラメータが指定された場合、クエリパラメータマップに双方が格納されて コアクライアントが実行されることを検証します。
     */
    @Test
    @DisplayName("正常系: from と to の双方が指定された場合、両パラメータを含むマップでコアクライアントを実行すること")
    void shouldFetchUserStampLogsWithBothFromAndTo() {
      // [Given]
      String from = "2026-04-01T00:00:00+09:00";
      String to = "2026-04-10T23:59:59+09:00";
      Map<String, String> expectedParams = Map.of("from", from, "to", to);

      HrmosStampLog mockLog = mock(HrmosStampLog.class);
      List<HrmosStampLog> expectedLogs = List.of(mockLog);

      given(coreClient.fetchAndParseList(
          eq(token),
          eq(expectedPath),
          eq(page),
          eq(expectedParams),
          eq("ユーザー打刻ログ"),
          any(TypeReference.class)
      )).willReturn(expectedLogs);

      // [When]
      List<HrmosStampLog> actualLogs = stampLogApi.fetchUserStampLogs(token, userId, from, to,
          page);

      // [Then]
      assertAll(
          () -> assertNotNull(actualLogs),
          () -> assertEquals(1, actualLogs.size()),
          () -> assertEquals(expectedLogs, actualLogs)
      );
      then(coreClient).should().fetchAndParseList(
          eq(token),
          eq(expectedPath),
          eq(page),
          eq(expectedParams),
          eq("ユーザー打刻ログ"),
          any(TypeReference.class)
      );
    }

    /**
     * from のみが指定され to が null の場合、from のみを含むマップで コアクライアントが実行されることを検証します（C0 分岐網羅）。
     */
    @Test
    @DisplayName("正常系: from のみ指定（to は null）の場合、from のみを含むマップで実行されること")
    void shouldFetchUserStampLogsWithFromOnly() {
      // [Given]
      String from = "2026-04-01T00:00:00+09:00";
      String to = null;
      Map<String, String> expectedParams = Map.of("from", from);

      given(coreClient.fetchAndParseList(
          eq(token),
          eq(expectedPath),
          eq(page),
          eq(expectedParams),
          eq("ユーザー打刻ログ"),
          any(TypeReference.class)
      )).willReturn(Collections.emptyList());

      // [When]
      List<HrmosStampLog> actualLogs = stampLogApi.fetchUserStampLogs(token, userId, from, to,
          page);

      // [Then]
      assertNotNull(actualLogs);
      then(coreClient).should().fetchAndParseList(
          eq(token),
          eq(expectedPath),
          eq(page),
          eq(expectedParams),
          eq("ユーザー打刻ログ"),
          any(TypeReference.class)
      );
    }

    /**
     * to のみが指定され from が null の場合、to のみを含むマップで コアクライアントが実行されることを検証します（C0 分岐網羅）。
     */
    @Test
    @DisplayName("正常系: to のみ指定（from は null）の場合、to のみを含むマップで実行されること")
    void shouldFetchUserStampLogsWithToOnly() {
      // [Given]
      String from = null;
      String to = "2026-04-10T23:59:59+09:00";
      Map<String, String> expectedParams = Map.of("to", to);

      given(coreClient.fetchAndParseList(
          eq(token),
          eq(expectedPath),
          eq(page),
          eq(expectedParams),
          eq("ユーザー打刻ログ"),
          any(TypeReference.class)
      )).willReturn(Collections.emptyList());

      // [When]
      List<HrmosStampLog> actualLogs = stampLogApi.fetchUserStampLogs(token, userId, from, to,
          page);

      // [Then]
      assertNotNull(actualLogs);
      then(coreClient).should().fetchAndParseList(
          eq(token),
          eq(expectedPath),
          eq(page),
          eq(expectedParams),
          eq("ユーザー打刻ログ"),
          any(TypeReference.class)
      );
    }

    /**
     * from と to の双方が null の場合、空マップが渡されてコアクライアントが実行されることを検証します（C0 分岐網羅）。
     */
    @Test
    @DisplayName("正常系: from と to が共に null の場合、空のマップで実行されること")
    void shouldFetchUserStampLogsWithoutFromAndTo() {
      // [Given]
      String from = null;
      String to = null;

      given(coreClient.fetchAndParseList(
          eq(token),
          eq(expectedPath),
          eq(page),
          eq(Collections.emptyMap()),
          eq("ユーザー打刻ログ"),
          any(TypeReference.class)
      )).willReturn(Collections.emptyList());

      // [When]
      List<HrmosStampLog> actualLogs = stampLogApi.fetchUserStampLogs(token, userId, from, to,
          page);

      // [Then]
      assertNotNull(actualLogs);
      then(coreClient).should().fetchAndParseList(
          eq(token),
          eq(expectedPath),
          eq(page),
          eq(Collections.emptyMap()),
          eq("ユーザー打刻ログ"),
          any(TypeReference.class)
      );
    }
  }
}
