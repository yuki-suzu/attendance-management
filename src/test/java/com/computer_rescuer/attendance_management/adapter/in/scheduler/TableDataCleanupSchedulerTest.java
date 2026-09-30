package com.computer_rescuer.attendance_management.adapter.in.scheduler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import com.computer_rescuer.attendance_management.application.port.out.CheckedEmployeeRepositoryPort;
import com.computer_rescuer.attendance_management.application.port.out.MonthlyAttendanceSummaryRepositoryPort;
import com.computer_rescuer.attendance_management.shared.TestClockFactory;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link TableDataCleanupScheduler} の単体テストクラス。
 * <p>
 * 保持期間を超過した不要履歴データのパージ処理において、注入された {@link Clock} を基準とした
 * 境界日（1年前・6ヶ月前）の算出、各永続化ポートの呼び出し、および各パージ処理が独立して安全に例外を捕捉する フェイルセーフ構造の命令網羅（C0 100%）を検証します。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TableDataCleanupScheduler 単体テスト")
class TableDataCleanupSchedulerTest {

  @Mock
  private CheckedEmployeeRepositoryPort checkedEmployeeRepositoryPort;

  @Mock
  private MonthlyAttendanceSummaryRepositoryPort summaryRepositoryPort;

  private Clock clock = TestClockFactory.fixedAt("2026-10-15", "12:00:00");

  private TableDataCleanupScheduler scheduler;

  @BeforeEach
  void setUp() {
    scheduler = new TableDataCleanupScheduler(checkedEmployeeRepositoryPort, summaryRepositoryPort,
        clock);
  }

  @Nested
  @DisplayName("データライフサイクル・パージ実行シナリオ")
  class PurgeExpiredRecordsScenario {

    /**
     * 正常系: 1年前（2025-10-15）以前のチェック済み履歴と、 6ヶ月前（2026-04-15 12:00:00）以前の月次サマリがそれぞれ削除されることを検証します。
     */
    @Test
    @DisplayName("正常系: 基準日に基づく1年前および6ヶ月前の境界値で両テーブルのパージ処理を実行すること")
    void shouldPurgeExpiredRecordsWithCalculatedThresholds() {
      // [Given]
      LocalDate expectedCheckedThreshold = LocalDate.of(2025, 10, 15);
      LocalDateTime expectedSummaryThreshold = LocalDateTime.of(2026, 4, 15, 12, 0, 0);

      given(checkedEmployeeRepositoryPort.deleteOlderThan(expectedCheckedThreshold)).willReturn(10);
      given(summaryRepositoryPort.deleteOlderThan(expectedSummaryThreshold)).willReturn(5);

      // [When]
      scheduler.purgeExpiredRecords();

      // [Then]
      then(checkedEmployeeRepositoryPort).should().deleteOlderThan(expectedCheckedThreshold);
      then(summaryRepositoryPort).should().deleteOlderThan(expectedSummaryThreshold);
    }

    /**
     * チェック済み履歴のパージで例外が発生した場合でも、例外が安全に catch ログ出力され、 後続の月次サマリパージが中断されずに実行されることを検証します。
     */
    @Test
    @DisplayName("異常系: チェック済み履歴のパージ失敗時でも、月次サマリのパージ処理が継続して実行されること")
    void shouldContinueMonthlySummaryPurgeEvenWhenCheckedEmployeePurgeFails() {
      // [Given]
      LocalDate expectedCheckedThreshold = LocalDate.of(2025, 10, 15);
      LocalDateTime expectedSummaryThreshold = LocalDateTime.of(2026, 4, 15, 12, 0, 0);

      willThrow(new RuntimeException("DBロック競合タイムアウト"))
          .given(checkedEmployeeRepositoryPort).deleteOlderThan(expectedCheckedThreshold);
      given(summaryRepositoryPort.deleteOlderThan(expectedSummaryThreshold)).willReturn(3);

      // [When & Then]
      assertDoesNotThrow(
          () -> scheduler.purgeExpiredRecords(),
          "パージ失敗時も上位スレッドへ例外を伝播させないこと"
      );
      then(checkedEmployeeRepositoryPort).should().deleteOlderThan(expectedCheckedThreshold);
      then(summaryRepositoryPort).should().deleteOlderThan(expectedSummaryThreshold);
    }

    /**
     * 月次サマリのパージで例外が発生した場合でも、スケジューラースレッドをクラッシュさせずに 安全に例外が捕捉されることを検証します。
     */
    @Test
    @DisplayName("異常系: 月次サマリのパージ失敗時でも例外が安全に捕捉され、正常終了すること")
    void shouldCatchExceptionWhenMonthlySummaryPurgeFails() {
      // [Given]
      LocalDate expectedCheckedThreshold = LocalDate.of(2025, 10, 15);
      LocalDateTime expectedSummaryThreshold = LocalDateTime.of(2026, 4, 15, 12, 0, 0);

      given(checkedEmployeeRepositoryPort.deleteOlderThan(expectedCheckedThreshold)).willReturn(10);
      willThrow(new RuntimeException("月次テーブル削除障害"))
          .given(summaryRepositoryPort).deleteOlderThan(expectedSummaryThreshold);

      // [When & Then]
      assertDoesNotThrow(
          () -> scheduler.purgeExpiredRecords(),
          "月次サマリパージの例外が外へ漏れないこと"
      );
      then(checkedEmployeeRepositoryPort).should().deleteOlderThan(expectedCheckedThreshold);
      then(summaryRepositoryPort).should().deleteOlderThan(expectedSummaryThreshold);
    }
  }
}
