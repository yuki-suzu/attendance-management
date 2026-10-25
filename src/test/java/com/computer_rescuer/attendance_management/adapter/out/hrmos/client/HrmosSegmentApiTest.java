package com.computer_rescuer.attendance_management.adapter.out.hrmos.client;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.computer_rescuer.attendance_management.adapter.out.hrmos.model.HrmosSegment;
import java.time.OffsetDateTime;
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
 * {@link HrmosSegmentApi} の単体テストクラス。
 * <p>
 * HRMOS 勤務区分 API ファサードとして、エンドポイントパス（"/segments"）および リソース名（"勤務区分"）の型安全な引き渡し検証（C0 100%）を行います。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HrmosSegmentApi 単体テスト")
class HrmosSegmentApiTest {

  @Mock
  private HrmosCoreHttpClient coreClient;

  @InjectMocks
  private HrmosSegmentApi segmentApi;

  @Nested
  @DisplayName("勤務区分一覧取得（fetchSegments）シナリオ")
  class FetchSegmentsScenario {

    /**
     * 正しい引数でコアクライアントが呼び出され、取得された勤務区分リストが返却されることを検証します。
     */
    @Test
    @DisplayName("正常系: 勤務区分エンドポイントに対してコアクライアントを呼び出し、勤務区分リストを返却すること")
    void shouldFetchSegmentsThroughCoreClient() {
      // [Given]
      String token = "dummy-token";
      HrmosSegment segment = new HrmosSegment(
          1, "出勤", "通常出勤", 1,
          OffsetDateTime.parse("2026-04-01T09:00:00+09:00"),
          OffsetDateTime.parse("2026-04-01T18:00:00+09:00")
      );
      List<HrmosSegment> expectedList = List.of(segment);

      given(coreClient.fetchAndParseList(
          eq(token),
          eq("/segments"),
          eq(1),
          eq("勤務区分"),
          any(TypeReference.class)
      )).willReturn(expectedList);

      // [When]
      List<HrmosSegment> actualList = segmentApi.fetchSegments(token);

      // [Then]
      assertAll(
          () -> assertNotNull(actualList, "返却リストが null でないこと"),
          () -> assertEquals(1, actualList.size(), "要素数が一致すること"),
          () -> assertEquals(expectedList, actualList, "取得結果が一致すること")
      );
      then(coreClient).should().fetchAndParseList(
          eq(token),
          eq("/segments"),
          eq(1),
          eq("勤務区分"),
          any(TypeReference.class)
      );
    }
  }
}
