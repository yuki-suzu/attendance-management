package com.computer_rescuer.attendance_management.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * {@link StampLog} の単体テストクラス。
 * <p>
 * 従業員の打刻事実モデルにおいて、打刻種別（{@link StampType}）に基づく
 * 出勤打刻判定（{@link StampLog#isClockIn()}）および退勤打刻判定（{@link StampLog#isClockOut()}）の 条件分岐・null
 * 安全性を検証します。
 * </p>
 */
@DisplayName("StampLog 単体テスト")
class StampLogTest {

  /**
   * テスト用の {@link StampLog} インスタンスを生成するヘルパーメソッド。
   *
   * @param stampType 打刻種別
   * @return 構築された {@link StampLog}
   */
  private StampLog createStampLog(StampType stampType) {
    return new StampLog(
        101,
        "EMP001",
        "山田",
        "太郎",
        LocalDateTime.of(2026, 4, 20, 8, 55, 0),
        stampType,
        "Mozilla/5.0"
    );
  }

  @Nested
  @DisplayName("isClockIn 出勤打刻判定シナリオ")
  class IsClockInScenario {

    /**
     * 【テスト観点 / 責務】 {@link StampLog#isClockIn()} の真偽値判定検証。 打刻種別が {@link StampType#CLOCK_IN} の場合のみ
     * {@code true} を返却し、 退勤打刻や休憩開始、未定義、null の場合は {@code false} を返却することを確認します（C1 分岐網羅）。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>各 {@link StampType} または null を持つ {@link StampLog} インスタンスを用意</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>CLOCK_IN のみ {@code true}、それ以外はすべて {@code false} であること</li>
     * </ul>
     */
    @Test
    @DisplayName("正常系・C1網羅: isClockIn は stampType が CLOCK_IN の場合のみ true を返却すること")
    void shouldIdentifyClockInCorrectly() {
      // [Given & When & Then]
      assertThat(createStampLog(StampType.CLOCK_IN).isClockIn()).isTrue();
      assertThat(createStampLog(StampType.CLOCK_OUT).isClockIn()).isFalse();
      assertThat(createStampLog(StampType.BREAK_START).isClockIn()).isFalse();
      assertThat(createStampLog(StampType.BREAK_END).isClockIn()).isFalse();
      assertThat(createStampLog(StampType.UNKNOWN).isClockIn()).isFalse();
      assertThat(createStampLog(null).isClockIn()).isFalse();
    }
  }

  @Nested
  @DisplayName("isClockOut 退勤打刻判定シナリオ")
  class IsClockOutScenario {

    /**
     * 【テスト観点 / 責務】 {@link StampLog#isClockOut()} の真偽値判定検証。 打刻種別が {@link StampType#CLOCK_OUT} の場合のみ
     * {@code true} を返却し、 出勤打刻や休憩開始、未定義、null の場合は {@code false} を返却することを確認します（C1 分岐網羅）。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>各 {@link StampType} または null を持つ {@link StampLog} インスタンスを用意</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>CLOCK_OUT のみ {@code true}、それ以外はすべて {@code false} であること</li>
     * </ul>
     */
    @Test
    @DisplayName("正常系・C1網羅: isClockOut は stampType が CLOCK_OUT の場合のみ true を返却すること")
    void shouldIdentifyClockOutCorrectly() {
      // [Given & When & Then]
      assertThat(createStampLog(StampType.CLOCK_OUT).isClockOut()).isTrue();
      assertThat(createStampLog(StampType.CLOCK_IN).isClockOut()).isFalse();
      assertThat(createStampLog(StampType.BREAK_START).isClockOut()).isFalse();
      assertThat(createStampLog(StampType.BREAK_END).isClockOut()).isFalse();
      assertThat(createStampLog(StampType.UNKNOWN).isClockOut()).isFalse();
      assertThat(createStampLog(null).isClockOut()).isFalse();
    }
  }
}
