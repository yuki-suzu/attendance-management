package com.computer_rescuer.attendance_management.adapter.in.scheduler;

import static org.mockito.BDDMockito.then;

import com.computer_rescuer.attendance_management.application.port.in.NotifyUnstampedAlertUseCase;
import com.computer_rescuer.attendance_management.shared.TestClockFactory;
import java.time.Clock;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link UnstampedAlertScheduler} の単体テストクラス。
 * <p>
 * 午前・正午の各定期実行タスクにおいて、注入された {@link Clock} から正確な本日日付（{@link LocalDate}）を取得し、
 * 未打刻アラート通知ユースケース（{@link NotifyUnstampedAlertUseCase}）へディスパッチする命令網羅（C0 100%）を検証します。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UnstampedAlertScheduler 単体テスト")
class UnstampedAlertSchedulerTest {

  @Mock
  private NotifyUnstampedAlertUseCase useCase;

  private Clock clock = TestClockFactory.fixedAt("2026-04-20", "10:00:00");

  private UnstampedAlertScheduler scheduler;

  private final LocalDate expectedDate = LocalDate.now(clock);

  @BeforeEach
  void setUp() {
    scheduler = new UnstampedAlertScheduler(useCase, clock);
  }

  @Nested
  @DisplayName("未打刻アラート定期実行シナリオ")
  class ExecuteDailyAlertScenario {

    /**
     * 午前タスク（runMorningTask）実行時に、当日日付でユースケースが呼び出されることを検証します。
     */
    @Test
    @DisplayName("午前タスク実行時、Clock に基づく当日付で未打刻通知ユースケースを実行すること")
    void shouldExecuteUseCaseOnMorningTask() {
      // [When]
      scheduler.runMorningTask();

      // [Then]
      then(useCase).should().execute(expectedDate);
    }

    /**
     * 正午タスク（runNoonTask）実行時に、当日日付でユースケースが呼び出されることを検証します。
     */
    @Test
    @DisplayName("正午タスク実行時、Clock に基づく当日付で未打刻通知ユースケースを実行すること")
    void shouldExecuteUseCaseOnNoonTask() {
      // [When]
      scheduler.runNoonTask();

      // [Then]
      then(useCase).should().execute(expectedDate);
    }
  }
}
