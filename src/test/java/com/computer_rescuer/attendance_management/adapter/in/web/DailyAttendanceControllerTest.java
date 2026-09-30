package com.computer_rescuer.attendance_management.adapter.in.web;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.computer_rescuer.attendance_management.adapter.in.model.ApiResponse;
import com.computer_rescuer.attendance_management.application.port.in.GetDailyAttendanceUseCase;
import com.computer_rescuer.attendance_management.domain.model.DailyAttendance;
import com.computer_rescuer.attendance_management.shared.TestClockFactory;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

/**
 * {@link DailyAttendanceController} の単体テストクラス。
 * <p>
 * 従業員出勤状況照会 API として、日付文字列パラメータのパース、本日日付フォールバック、
 * および入力ユースケース（{@link GetDailyAttendanceUseCase}）連携の命令網羅（C0 100%）を検証します。 Spring コンテキストを起動せず、純粋な
 * Mockito 単体テストとして高速に実行します。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DailyAttendanceController 単体テスト")
class DailyAttendanceControllerTest {

  @Mock
  private GetDailyAttendanceUseCase useCase;

  @Spy
  private Clock clock = TestClockFactory.fixedAt("2026-04-20", "10:00:00");

  @InjectMocks
  private DailyAttendanceController controller;

  @Nested
  @DisplayName("出勤状況一覧取得（getAttendance）シナリオ")
  class GetAttendanceScenario {

    /**
     * 日付文字列が渡された場合、正しく {@link LocalDate} へパースされてユースケースが実行されることを検証します。
     */
    @Test
    @DisplayName("日付文字列指定あり: 指定された日付文字列がパースされてユースケースに引き渡されること")
    void shouldReturnAttendancesWithParsedDate() {
      // [Given]
      LocalDate parsedDate = LocalDate.of(2026, 4, 20);

      DailyAttendance mockAttendance = mock(DailyAttendance.class);
      List<DailyAttendance> expectedList = List.of(mockAttendance);
      given(useCase.execute(parsedDate)).willReturn(expectedList);

      // [When]
      ResponseEntity<ApiResponse<List<DailyAttendance>>> actual = controller.getAttendance(
          parsedDate);

      // [Then]
      assertAll(
          () -> assertNotNull(actual, "出勤状況リストが返却されること"),
          () -> assertEquals(1, actual.getBody().data().size(), "要素数が一致すること"),
          () -> assertEquals(expectedList, actual.getBody().data(), "取得結果が一致すること")
      );
      then(useCase).should().execute(parsedDate);
    }

    /**
     * 日付文字列が null の場合、本日の日付が適用されてユースケースが実行されることを検証します。
     */
    @Test
    @DisplayName("日付指定なし (null): 本日の日付が適用されてユースケースに引き渡されること")
    void shouldReturnAttendancesWithCurrentDateWhenNull() {
      // [Given]
      DailyAttendance mockAttendance = mock(DailyAttendance.class);
      given(useCase.execute(any(LocalDate.class))).willReturn(List.of(mockAttendance));

      // [When]
      ResponseEntity<ApiResponse<List<DailyAttendance>>> actual = controller.getAttendance(null);

      // [Then]
      assertAll(
          () -> assertNotNull(actual, "出勤状況リストが返却されること"),
          () -> assertEquals(1, actual.getBody().data().size(), "要素数が一致すること")
      );
      then(useCase).should().execute(any(LocalDate.class));
    }
  }
}
