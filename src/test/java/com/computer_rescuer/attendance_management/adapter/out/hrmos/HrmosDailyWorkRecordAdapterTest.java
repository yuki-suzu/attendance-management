package com.computer_rescuer.attendance_management.adapter.out.hrmos;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.computer_rescuer.attendance_management.adapter.out.hrmos.client.HrmosAuthApi;
import com.computer_rescuer.attendance_management.adapter.out.hrmos.client.HrmosWorkOutputApi;
import com.computer_rescuer.attendance_management.adapter.out.hrmos.mapper.HrmosWorkOutputMapper;
import com.computer_rescuer.attendance_management.adapter.out.hrmos.model.HrmosDailyWorkOutput;
import com.computer_rescuer.attendance_management.domain.model.DailyWorkRecord;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link HrmosDailyWorkRecordAdapter} の単体テストクラス。
 * <p>
 * 日次勤怠実績取得出力ポートの実装として、認証トークンの取得、 ページネーションヘルパーを介した勤怠実績 API 呼び出し、および ドメインモデルへのマッピング連携における命令網羅（C0
 * 100%）を検証します。 Spring コンテキストを起動せず、純粋な Mockito 単体テストとして高速に実行します。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HrmosDailyWorkRecordAdapter 単体テスト")
class HrmosDailyWorkRecordAdapterTest {

  @Mock
  private HrmosAuthApi authApi;

  @Mock
  private HrmosWorkOutputApi workOutputApi;

  @Mock
  private HrmosWorkOutputMapper mapper;

  @InjectMocks
  private HrmosDailyWorkRecordAdapter adapter;

  private final String token = "test-auth-token";
  private final LocalDate targetDate = LocalDate.of(2026, 4, 20);

  @Nested
  @DisplayName("日次勤怠実績取得（fetchByDate）シナリオ")
  class FetchByDateScenario {

    /**
     * 有効な対象日を指定した場合、認証トークンが取得され、ページネーションヘルパー経由で API が呼び出された後、ドメインモデルリストへマッピングされて返却されることを検証します。
     */
    @Test
    @DisplayName("正常系: 対象日付の日次勤怠を取得し、ドメインモデルのリストとして返却すること")
    void shouldFetchAndMapDailyWorkRecords() {
      // [Given]
      given(authApi.fetchToken()).willReturn(token);

      HrmosDailyWorkOutput rawOutput = mock(HrmosDailyWorkOutput.class);
      List<HrmosDailyWorkOutput> rawList = List.of(rawOutput);
      // 1 ページ目で 1 件返却（100件未満のためヘルパーが 1 ページで終了）
      given(workOutputApi.fetchDailyWorkOutputs(token, "2026-04-20", 1))
          .willReturn(rawList);

      DailyWorkRecord domainRecord = mock(DailyWorkRecord.class);
      List<DailyWorkRecord> expectedRecords = List.of(domainRecord);
      given(mapper.toDomainList(rawList)).willReturn(expectedRecords);

      // [When]
      List<DailyWorkRecord> actualRecords = adapter.fetchByDate(targetDate);

      // [Then]
      assertAll(
          () -> assertNotNull(actualRecords, "返却リストが null でないこと"),
          () -> assertEquals(1, actualRecords.size(), "件数が一致すること"),
          () -> assertEquals(expectedRecords, actualRecords, "取得結果が一致すること")
      );
      then(authApi).should().fetchToken();
      then(workOutputApi).should().fetchDailyWorkOutputs(token, "2026-04-20", 1);
      then(mapper).should().toDomainList(rawList);
    }
  }
}
