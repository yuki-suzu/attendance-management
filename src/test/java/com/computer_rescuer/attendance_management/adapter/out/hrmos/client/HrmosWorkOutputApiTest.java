package com.computer_rescuer.attendance_management.adapter.out.hrmos.client;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.computer_rescuer.attendance_management.adapter.out.hrmos.model.HrmosDailyWorkOutput;
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
 * {@link HrmosWorkOutputApi} の単体テストクラス。
 * <p>
 * HRMOS 勤怠実績 API ファサードとして、全従業員日次勤怠エンドポイントへの要求委譲、 およびユーザー別月次勤怠エンドポイントにおける user_id クエリパラメータの構築と コア
 * HTTP クライアント（{@link HrmosCoreHttpClient}）連携の命令網羅（C0 100%）を検証します。 Spring コンテキストを起動せず、純粋な Mockito
 * 単体テストとして高速に実行します。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HrmosWorkOutputApi 単体テスト")
class HrmosWorkOutputApiTest {

  @Mock
  private HrmosCoreHttpClient coreClient;

  @InjectMocks
  private HrmosWorkOutputApi workOutputApi;

  private final String token = "dummy-auth-token";

  @Nested
  @DisplayName("全従業員日次勤怠実績取得（fetchDailyWorkOutputs）シナリオ")
  class FetchDailyWorkOutputsScenario {

    /**
     * 指定日を含むパス（/work_outputs/daily/{date}）が構築され、 コアクライアントから取得した勤怠生データリストが返却されることを検証します。
     */
    @Test
    @DisplayName("正常系: 対象日を含むパスでコアクライアントを実行し、日次勤怠リストを返却すること")
    void shouldFetchDailyWorkOutputsThroughCoreClient() {
      // [Given]
      String targetDate = "2026-04-20";
      int page = 1;
      String expectedPath = "/work_outputs/daily/2026-04-20";

      HrmosDailyWorkOutput mockOutput = mock(HrmosDailyWorkOutput.class);
      List<HrmosDailyWorkOutput> expectedList = List.of(mockOutput);

      given(coreClient.fetchAndParseList(
          eq(token),
          eq(expectedPath),
          eq(page),
          eq("日次勤怠"),
          any(TypeReference.class)
      )).willReturn(expectedList);

      // [When]
      List<HrmosDailyWorkOutput> actualList = workOutputApi.fetchDailyWorkOutputs(token, targetDate,
          page);

      // [Then]
      assertAll(
          () -> assertNotNull(actualList, "返却リストが null でないこと"),
          () -> assertEquals(1, actualList.size(), "件数が一致すること"),
          () -> assertEquals(expectedList, actualList, "取得結果が一致すること")
      );
      then(coreClient).should().fetchAndParseList(
          eq(token),
          eq(expectedPath),
          eq(page),
          eq("日次勤怠"),
          any(TypeReference.class)
      );
    }
  }

  @Nested
  @DisplayName("ユーザー別月次勤怠実績取得（fetchMonthlyWorkOutputsByUser）シナリオ")
  class FetchMonthlyWorkOutputsByUserScenario {

    /**
     * 指定された月（month）からパスが構築され、ユーザーID（user_id）を含むクエリパラメータマップとともに ページ 1 固定でコアクライアントが呼び出されることを検証します。
     */
    @Test
    @DisplayName("正常系: 対象月パスおよび user_id クエリパラメータでコアクライアントを実行し、月次勤怠リストを返却すること")
    void shouldFetchMonthlyWorkOutputsByUserThroughCoreClient() {
      // [Given]
      String targetMonth = "2026-04";
      Integer userId = 205;
      String expectedPath = "/work_outputs/monthly/2026-04";
      Map<String, String> expectedQueryParams = Map.of("user_id", "205");

      HrmosDailyWorkOutput mockOutput = mock(HrmosDailyWorkOutput.class);
      List<HrmosDailyWorkOutput> expectedList = List.of(mockOutput);

      given(coreClient.fetchAndParseList(
          eq(token),
          eq(expectedPath),
          eq(1),
          eq(expectedQueryParams),
          eq("月次勤怠"),
          any(TypeReference.class)
      )).willReturn(expectedList);

      // [When]
      List<HrmosDailyWorkOutput> actualList = workOutputApi.fetchMonthlyWorkOutputsByUser(token,
          targetMonth, userId);

      // [Then]
      assertAll(
          () -> assertNotNull(actualList, "返却リストが null でないこと"),
          () -> assertEquals(1, actualList.size(), "件数が一致すること"),
          () -> assertEquals(expectedList, actualList, "取得結果が一致すること")
      );
      then(coreClient).should().fetchAndParseList(
          eq(token),
          eq(expectedPath),
          eq(1),
          eq(expectedQueryParams),
          eq("月次勤怠"),
          any(TypeReference.class)
      );
    }
  }
}
