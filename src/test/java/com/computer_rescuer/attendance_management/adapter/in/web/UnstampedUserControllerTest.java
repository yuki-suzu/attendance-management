package com.computer_rescuer.attendance_management.adapter.in.web;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.computer_rescuer.attendance_management.adapter.in.model.ApiResponse;
import com.computer_rescuer.attendance_management.application.port.in.ExtractUnstampedUsersUseCase;
import com.computer_rescuer.attendance_management.domain.model.DailyAttendance;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * {@link UnstampedUserController} の単体テストクラス。
 * <p>
 * 未打刻従業員照会 API として、対象日指定の有無に応じた日付解決（{@link Clock} からの当日フォールバック）、
 * および入力ユースケース（{@link ExtractUnstampedUsersUseCase}）連携の命令網羅（C0 100%）を検証します。 Spring コンテキストを起動せず、純粋な
 * Mockito 単体テストとして高速に実行します。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UnstampedUserController 単体テスト")
class UnstampedUserControllerTest {

  @Mock
  private ExtractUnstampedUsersUseCase useCase;

  private Clock fixedClock;

  private UnstampedUserController controller;

  private final ZoneId zoneId = ZoneId.of("Asia/Tokyo");
  // 基準日: 2026年4月20日
  private final Instant fixedInstant = Instant.parse("2026-04-20T01:00:00Z");
  private final LocalDate today = LocalDate.of(2026, 4, 20);

  @BeforeEach
  void setUp() {
    fixedClock = Clock.fixed(fixedInstant, zoneId);
    controller = new UnstampedUserController(useCase, fixedClock);
  }

  @Nested
  @DisplayName("未打刻者一覧取得（getUnstampedUsers）シナリオ")
  class GetUnstampedUsersScenario {

    /**
     * 日付パラメータが明示的に指定された場合、その日付を用いてユースケースが実行され、 HTTP 200 (OK) と未打刻者リストが返却されることを検証します。
     */
    @Test
    @DisplayName("日付指定あり: 指定された日付で未打刻者抽出ユースケースを実行し、200 OK レスポンスを返却すること")
    void shouldGetUnstampedUsersWithSpecifiedDate() {
      // [Given]
      LocalDate specifiedDate = LocalDate.of(2026, 4, 15);
      DailyAttendance mockAttendance = mock(DailyAttendance.class);
      List<DailyAttendance> expectedList = List.of(mockAttendance);
      given(useCase.execute(specifiedDate)).willReturn(expectedList);

      // [When]
      ResponseEntity<ApiResponse<List<DailyAttendance>>> response = controller.getUnstampedUsers(
          specifiedDate);

      // [Then]
      assertAll(
          () -> assertEquals(HttpStatus.OK, response.getStatusCode(),
              "HTTP ステータスは 200 であること"),
          () -> assertNotNull(response.getBody(), "レスポンスボディが存在すること"),
          () -> assertEquals(1, response.getBody().data().size(), "件数が一致すること"),
          () -> assertEquals(expectedList, response.getBody().data(), "返却リストが一致すること")
      );
      then(useCase).should().execute(specifiedDate);
    }

    /**
     * 日付パラメータが省略（null）された場合、Clock に基づく本日の日付が適用されて ユースケースが実行されることを検証します。
     */
    @Test
    @DisplayName("日付指定なし (null): Clock に基づく本日の日付が適用されて未打刻者抽出ユースケースを実行すること")
    void shouldGetUnstampedUsersWithTodayWhenDateIsNull() {
      // [Given]
      LocalDate nullDate = null;
      DailyAttendance mockAttendance = mock(DailyAttendance.class);
      List<DailyAttendance> expectedList = List.of(mockAttendance);
      given(useCase.execute(today)).willReturn(expectedList);

      // [When]
      ResponseEntity<ApiResponse<List<DailyAttendance>>> response = controller.getUnstampedUsers(
          nullDate);

      // [Then]
      assertAll(
          () -> assertEquals(HttpStatus.OK, response.getStatusCode(),
              "HTTP ステータスは 200 であること"),
          () -> assertNotNull(response.getBody(), "レスポンスボディが存在すること"),
          () -> assertEquals(1, response.getBody().data().size(), "件数が一致すること")
      );
      then(useCase).should().execute(today);
    }
  }
}
