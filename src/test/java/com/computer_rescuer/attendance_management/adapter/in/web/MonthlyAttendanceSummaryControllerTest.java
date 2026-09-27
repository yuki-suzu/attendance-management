package com.computer_rescuer.attendance_management.adapter.in.web;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;

import com.computer_rescuer.attendance_management.adapter.in.model.ApiResponse;
import com.computer_rescuer.attendance_management.adapter.in.model.ResultCode;
import com.computer_rescuer.attendance_management.application.port.in.MonthlyAttendanceSummaryUseCase;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * {@link MonthlyAttendanceSummaryController} の単体テストクラス。
 * <p>
 * 月次勤怠サマリ手動実行 API として、対象年月文字列のパース、未指定時の {@link Clock} からの
 * 当月フォールバック、およびユースケース（{@link MonthlyAttendanceSummaryUseCase}）実行の命令網羅（C0 100%）を検証します。 Spring
 * コンテキストを起動せず、純粋な Mockito 単体テストとして高速に実行します。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MonthlyAttendanceSummaryController 単体テスト")
class MonthlyAttendanceSummaryControllerTest {

  @Mock
  private MonthlyAttendanceSummaryUseCase useCase;

  private Clock fixedClock;

  private MonthlyAttendanceSummaryController controller;

  private final ZoneId zoneId = ZoneId.of("Asia/Tokyo");
  // 基準日: 2026年4月20日
  private final Instant fixedInstant = Instant.parse("2026-04-20T01:00:00Z");
  private final YearMonth currentYearMonth = YearMonth.of(2026, 4);

  @BeforeEach
  void setUp() {
    fixedClock = Clock.fixed(fixedInstant, zoneId);
    controller = new MonthlyAttendanceSummaryController(useCase, fixedClock);
  }

  @Nested
  @DisplayName("月次サマリ手動トリガー（triggerMonthlySummary）シナリオ")
  class TriggerMonthlySummaryScenario {

    /**
     * 年月文字列が明示的に指定された場合、その文字列がパースされてユースケースに引き渡され、 成功レスポンスが返却されることを検証します。
     */
    @Test
    @DisplayName("年月指定あり: 指定された年月（2026-03）で集計ユースケースが実行されること")
    void shouldTriggerMonthlySummaryWithSpecifiedMonth() {
      // [Given]
      String monthString = "2026-03";
      YearMonth expectedYearMonth = YearMonth.of(2026, 3);
      willDoNothing().given(useCase).execute(expectedYearMonth);

      // [When]
      ResponseEntity<ApiResponse<String>> response = controller.triggerMonthlySummary(monthString);

      // [Then]
      assertAll(
          () -> assertEquals(HttpStatus.OK, response.getStatusCode(),
              "HTTP ステータスは 200 であること"),
          () -> assertNotNull(response.getBody(), "レスポンスボディが存在すること"),
          () -> assertEquals(ResultCode.SUCCESS.name(), response.getBody().code(),
              "成功コードが返却されること"),
          () -> {
            String message = response.getBody().data();
            assertNotNull(message, "メッセージデータが存在すること");
            assertTrue(message.contains("2026-03"), "指定した年月がメッセージに含まれること");
          }
      );
      then(useCase).should().execute(expectedYearMonth);
    }

    /**
     * 年月文字列が null、空文字、または空白の場合、Clock に基づく当月（2026-04）が適用されて ユースケースが実行されることを検証します。
     */
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("年月未指定 (null/空文字/空白): Clock に基づく当月が適用されて集計ユースケースが実行されること")
    void shouldTriggerMonthlySummaryWithCurrentMonthWhenMonthIsNullOrBlank(String blankMonth) {
      // [Given]
      willDoNothing().given(useCase).execute(currentYearMonth);

      // [When]
      ResponseEntity<ApiResponse<String>> response = controller.triggerMonthlySummary(blankMonth);

      // [Then]
      assertAll(
          () -> assertEquals(HttpStatus.OK, response.getStatusCode(),
              "HTTP ステータスは 200 であること"),
          () -> assertNotNull(response.getBody(), "レスポンスボディが存在すること"),
          () -> {
            String message = response.getBody().data();
            assertNotNull(message, "メッセージデータが存在すること");
            assertTrue(message.contains("2026-04"),
                "Clock から解決された当月がメッセージに含まれること");
          }
      );
      then(useCase).should().execute(currentYearMonth);
    }
  }
}
