package com.computer_rescuer.attendance_management.adapter.in.scheduler;

import static org.mockito.BDDMockito.then;

import com.computer_rescuer.attendance_management.application.port.in.MonthlyAttendanceSummaryUseCase;
import com.computer_rescuer.attendance_management.shared.TestClockFactory;
import java.time.Clock;
import java.time.YearMonth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link MonthlyAttendanceSummaryScheduler} の単体テストクラス。
 * <p>
 * 定期実行スケジューラーが注入された {@link Clock} から正確な対象年月を導出し、
 * 月次サマリ集計ユースケース（{@link MonthlyAttendanceSummaryUseCase}）をトリガーする命令網羅（C0 100%）を検証します。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MonthlyAttendanceSummaryScheduler 単体テスト")
class MonthlyAttendanceSummarySchedulerTest {

  @Mock
  private MonthlyAttendanceSummaryUseCase useCase;

  private Clock clock = TestClockFactory.fixedAt("2026-04-15", "10:00:00");

  private MonthlyAttendanceSummaryScheduler scheduler;

  @BeforeEach
  void setUp() {
    scheduler = new MonthlyAttendanceSummaryScheduler(useCase, clock);
  }

  @Nested
  @DisplayName("定期集計実行シナリオ")
  class ExecuteDailySummaryScenario {

    /**
     * スケジュール実行時に、注入された Clock に基づく正確な YearMonth が算出され、 ユースケースの引数として渡されることを検証します。
     */
    @Test
    @DisplayName("Clock に基づく当月（2026-04）を引数として、月次サマリユースケースを実行すること")
    void shouldExecuteUseCaseWithCurrentYearMonthFromClock() {
      // [Given]
      YearMonth expectedYearMonth = YearMonth.of(2026, 4);

      // [When]
      scheduler.executeDailySummary();

      // [Then]
      then(useCase).should().execute(expectedYearMonth);
    }
  }
}
