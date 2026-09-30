package com.computer_rescuer.attendance_management.adapter.in.web;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.computer_rescuer.attendance_management.adapter.in.exception.InvalidRequestParameterException;
import com.computer_rescuer.attendance_management.adapter.in.model.ApiResponse;
import com.computer_rescuer.attendance_management.application.port.in.GetStampLogsUseCase;
import com.computer_rescuer.attendance_management.domain.model.StampLog;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

/**
 * {@link StampLogsController} の単体テストクラス。
 * <p>
 * 打刻履歴照会 API として、日次打刻履歴取得における対象日フォールバック、
 * ユーザー別打刻履歴取得における日付逆転相関バリデーション、および入力ユースケース（{@link GetStampLogsUseCase}）連携の 命令網羅（C0 100%）を検証します。
 * Spring コンテキストを起動せず、純粋な Mockito 単体テストとして高速に実行します。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StampLogsController 単体テスト")
class StampLogsControllerTest {

  @Mock
  private GetStampLogsUseCase useCase;

  private Clock fixedClock;

  private StampLogsController controller;

  private final ZoneId zoneId = ZoneId.of("Asia/Tokyo");
  // 基準日: 2026年4月20日
  private final Instant fixedInstant = Instant.parse("2026-04-20T01:00:00Z");
  private final LocalDate today = LocalDate.of(2026, 4, 20);

  @BeforeEach
  void setUp() {
    fixedClock = Clock.fixed(fixedInstant, zoneId);
    controller = new StampLogsController(useCase, fixedClock);
  }

  @Nested
  @DisplayName("全従業員の日次打刻履歴取得（getDaily）シナリオ")
  class GetDailyScenario {

    /**
     * 対象日付が明示的に指定された場合、その日付を用いてユースケースが実行されることを検証します。
     */
    @Test
    @DisplayName("日付指定あり: 指定された日付で日次打刻履歴取得ユースケースを実行すること")
    void shouldGetDailyLogsWithSpecifiedDate() {
      // [Given]
      LocalDate specifiedDate = LocalDate.of(2026, 4, 1);
      StampLog mockLog = mock(StampLog.class);
      List<StampLog> expectedLogs = List.of(mockLog);
      given(useCase.getDailyLogs(specifiedDate)).willReturn(expectedLogs);

      // [When]
      List<StampLog> actual = controller.getDaily(specifiedDate);

      // [Then]
      assertAll(
          () -> assertNotNull(actual, "打刻ログリストが返却されること"),
          () -> assertEquals(1, actual.size(), "件数が一致すること"),
          () -> assertEquals(expectedLogs, actual, "取得結果が一致すること")
      );
      then(useCase).should().getDailyLogs(specifiedDate);
    }

    /**
     * 対象日付が省略（null）された場合、Clock に基づく本日の日付が適用されてユースケースが実行されることを検証します。
     */
    @Test
    @DisplayName("日付指定なし (null): Clock に基づく本日の日付で日次打刻履歴取得ユースケースを実行すること")
    void shouldGetDailyLogsWithTodayWhenDateIsNull() {
      // [Given]
      LocalDate nullDate = null;
      StampLog mockLog = mock(StampLog.class);
      List<StampLog> expectedLogs = List.of(mockLog);
      given(useCase.getDailyLogs(today)).willReturn(expectedLogs);

      // [When]
      List<StampLog> actual = controller.getDaily(nullDate);

      // [Then]
      assertAll(
          () -> assertNotNull(actual, "打刻ログリストが返却されること"),
          () -> assertEquals(1, actual.size(), "件数が一致すること")
      );
      then(useCase).should().getDailyLogs(today);
    }
  }

  @Nested
  @DisplayName("特定ユーザーの期間内打刻履歴取得（getByUser）シナリオ")
  class GetByUserScenario {

    private final String employeeNumber = "EMP001";

    /**
     * 開始日と終了日の期間整合性が正しい場合、ユースケースが実行されてリストが返却されることを検証します。
     */
    @Test
    @DisplayName("正常系: 正常な日付期間（開始日 <= 終了日）でユーザー打刻履歴取得ユースケースを実行すること")
    void shouldGetUserLogsWithValidDateRange() {
      // [Given]
      LocalDate fromDate = LocalDate.of(2026, 4, 1);
      LocalDate toDate = LocalDate.of(2026, 4, 10);
      StampLog mockLog = mock(StampLog.class);
      List<StampLog> expectedLogs = List.of(mockLog);
      given(useCase.getUserLogs(employeeNumber, fromDate, toDate)).willReturn(expectedLogs);

      // [When]
      ResponseEntity<ApiResponse<List<StampLog>>> actual = controller.getByUser(employeeNumber,
          fromDate, toDate);

      // [Then]
      assertAll(
          () -> assertNotNull(actual, "打刻ログリストが返却されること"),
          () -> assertEquals(1, actual.getBody().data().size(), "件数が一致すること"),
          () -> assertEquals(expectedLogs, actual.getBody().data(), "取得結果が一致すること")
      );
      then(useCase).should().getUserLogs(employeeNumber, fromDate, toDate);
    }

    /**
     * 片方または両方の日付が null の場合でも、逆転チェックをスルーして安全にユースケースが実行されることを検証します。
     */
    @Test
    @DisplayName("正常系: 開始日または終了日が null の場合でも、例外をスローせずユースケースを実行すること")
    void shouldGetUserLogsWhenDatesAreNull() {
      // [Given]
      LocalDate fromDate = null;
      LocalDate toDate = LocalDate.of(2026, 4, 10);
      StampLog mockLog = mock(StampLog.class);
      List<StampLog> expectedLogs = List.of(mockLog);
      given(useCase.getUserLogs(employeeNumber, fromDate, toDate)).willReturn(expectedLogs);

      // [When]
      ResponseEntity<ApiResponse<List<StampLog>>> actual = controller.getByUser(employeeNumber,
          fromDate, toDate);

      // [Then]
      assertNotNull(actual, "打刻ログリストが返却されること");
      then(useCase).should().getUserLogs(employeeNumber, fromDate, toDate);
    }

    /**
     * 開始日(fromDate)が終了日(toDate)より未来に設定されている場合、 即座に InvalidRequestParameterException
     * がスローされることを検証します。
     */
    @Test
    @DisplayName("異常系: 開始日が終了日より未来の場合、InvalidRequestParameterException をスローすること")
    void shouldThrowInvalidRequestParameterExceptionWhenFromDateIsAfterToDate() {
      // [Given]
      LocalDate fromDate = LocalDate.of(2026, 4, 15);
      LocalDate toDate = LocalDate.of(2026, 4, 10);

      // [When & Then]
      InvalidRequestParameterException exception = assertThrows(
          InvalidRequestParameterException.class,
          () -> controller.getByUser(employeeNumber, fromDate, toDate),
          "日付逆転時はリクエストパラメータ例外がスローされること"
      );

      assertEquals(
          "開始日(fromDate)が終了日(toDate)より未来に設定されています。",
          exception.getMessage(),
          "例外メッセージが意図通りであること"
      );
      then(useCase).shouldHaveNoInteractions();
    }
  }
}
