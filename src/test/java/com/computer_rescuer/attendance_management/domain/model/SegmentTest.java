package com.computer_rescuer.attendance_management.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * {@link Segment} の単体テストクラス。
 * <p>
 * 勤務区分マスタの状態コード（{@code status}）に基づく出勤日判定ロジック（{@link Segment#isWorkingDay()}）、 境界値（1以外のステータスコード）、および
 * null 安全性を検証します。
 * </p>
 */
@DisplayName("Segment 単体テスト")
class SegmentTest {

  @Nested
  @DisplayName("isWorkingDay 出勤日判定シナリオ")
  class IsWorkingDayScenario {

    /**
     * 【テスト観点 / 責務】 正常系検証。勤務区分マスタのステータスが {@code 1}（勤務）である場合、 出勤日であると判定して {@code true}
     * を返却することを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>{@code status = 1}（通常勤務）の {@link Segment} インスタンスを用意</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>{@link Segment#isWorkingDay()} が {@code true} であること</li>
     * </ul>
     */
    @Test
    @DisplayName("正常系: status が 1 (勤務) の場合、isWorkingDay は true を返却すること")
    void shouldReturnTrueWhenStatusIsOne() {
      // [Given]
      Segment segment = new Segment(
          1, "通常出勤", "出勤", 1, LocalTime.of(9, 0), LocalTime.of(18, 0));

      // [When & Then]
      assertThat(segment.isWorkingDay()).isTrue();
    }

    /**
     * 【テスト観点 / 責務】 境界値・C1 分岐検証。ステータスが {@code 1} 以外（2: 休日・休暇, 3: その他, 未定義コード等）の場合、 出勤対象日ではないと判定して
     * {@code false} を返却することを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>{@code status} に 1 以外の整数（0, 2, 3, 4, 99, -1）を設定した {@link Segment} を用意</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>{@link Segment#isWorkingDay()} が {@code false} であること</li>
     * </ul>
     *
     * @param nonWorkingStatus 勤務（1）以外の状態コード
     */
    @ParameterizedTest
    @ValueSource(ints = {0, 2, 3, 4, 99, -1})
    @DisplayName("境界値: status が 1 以外 (休日・休暇・その他) の場合、isWorkingDay は false を返却すること")
    void shouldReturnFalseWhenStatusIsNotOne(int nonWorkingStatus) {
      // [Given]
      Segment segment = new Segment(
          2, "公休", "休日", nonWorkingStatus, null, null);

      // [When & Then]
      assertThat(segment.isWorkingDay()).isFalse();
    }

    /**
     * 【テスト観点 / 責務】 境界値・null 安全性検証。マスタの不整合等により {@code status} が {@code null} の場合でも、
     * {@link NullPointerException} を発生させずに安全に {@code false} を返却することを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>{@code status = null} の {@link Segment} インスタンスを用意</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>{@link Segment#isWorkingDay()} が {@code false} であること</li>
     * </ul>
     */
    @Test
    @DisplayName("境界値: status が null の場合、NullPointerException を起こさず false を返却すること")
    void shouldReturnFalseWhenStatusIsNull() {
      // [Given]
      Segment segment = new Segment(
          99, "未確定", "未確定", null, null, null);

      // [When & Then]
      assertThat(segment.isWorkingDay()).isFalse();
    }
  }
}
