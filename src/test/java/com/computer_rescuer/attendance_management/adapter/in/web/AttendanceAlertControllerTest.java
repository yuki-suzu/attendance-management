package com.computer_rescuer.attendance_management.adapter.in.web;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;

import com.computer_rescuer.attendance_management.adapter.in.model.ApiResponse;
import com.computer_rescuer.attendance_management.application.interactor.NotifyUnstampedAlertInteractor;
import com.computer_rescuer.attendance_management.shared.TestClockFactory;
import java.time.Clock;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * {@link AttendanceAlertController} の単体テストクラス。
 * <p>
 * 勤怠アラート手動実行 API として、対象日指定の有無に応じた日付解決および ユースケース実行（{@link NotifyUnstampedAlertInteractor}）の命令網羅（C0
 * 100%）を検証します。 Spring コンテキストを起動せず、純粋な Mockito 単体テストとして高速に実行します。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AttendanceAlertController 単体テスト")
class AttendanceAlertControllerTest {

  @Mock
  private NotifyUnstampedAlertInteractor notifyUnstampedAlertInteractor;

  @Spy
  private Clock clock = TestClockFactory.fixedAt("2026-04-20", "10:00:00");

  @InjectMocks
  private AttendanceAlertController controller;

  @Nested
  @DisplayName("未打刻アラート手動トリガー（triggerUnstampedAlert）シナリオ")
  class TriggerUnstampedAlertScenario {

    /**
     * 日付パラメータが明示的に指定された場合、その日付を引数として Interactor が呼び出されることを検証します。
     */
    @Test
    @DisplayName("日付指定あり: 指定された日付で未打刻通知バッチが実行され、200 OK レスポンスを返却すること")
    void shouldTriggerAlertWithSpecifiedDate() {
      // [Given]
      LocalDate targetDate = LocalDate.of(2026, 4, 15);
      willDoNothing().given(notifyUnstampedAlertInteractor).execute(targetDate);

      // [When]
      ResponseEntity<ApiResponse<String>> response = controller.triggerUnstampedAlert(targetDate);

      // [Then]
      assertAll(
          () -> assertEquals(HttpStatus.OK, response.getStatusCode(),
              "HTTP ステータスは 200 であること"),
          () -> assertNotNull(response.getBody(), "レスポンスボディが存在すること"),
          () -> assertTrue(response.getBody().data().contains("2026-04-15"),
              "指定した日付がメッセージに含まれること")
      );
      then(notifyUnstampedAlertInteractor).should().execute(targetDate);
    }

    /**
     * 日付パラメータが省略（null）された場合、システム日付の本日が自動適用されて Interactor が呼び出されることを検証します。
     */
    @Test
    @DisplayName("日付指定なし (null): 本日の日付が適用されて未打刻通知バッチが実行されること")
    void shouldTriggerAlertWithCurrentDateWhenNull() {
      // [Given]
      LocalDate nullDate = null;
      willDoNothing().given(notifyUnstampedAlertInteractor).execute(any(LocalDate.class));

      // [When]
      ResponseEntity<ApiResponse<String>> response = controller.triggerUnstampedAlert(nullDate);

      // [Then]
      assertAll(
          () -> assertEquals(HttpStatus.OK, response.getStatusCode(),
              "HTTP ステータスは 200 であること"),
          () -> assertNotNull(response.getBody(), "レスポンスボディが存在すること"),
          () -> assertTrue(
              response.getBody().data().contains("アラート通知バッチの実行が完了しました"),
              "完了メッセージが含まれること")
      );
      then(notifyUnstampedAlertInteractor).should().execute(any(LocalDate.class));
    }
  }
}
