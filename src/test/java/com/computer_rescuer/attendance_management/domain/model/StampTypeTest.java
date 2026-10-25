package com.computer_rescuer.attendance_management.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * {@link StampType} の単体テストクラス。
 * <p>
 * HRMOS API 仕様に基づく打刻区分コードの逆引き変換ロジック（{@link StampType#fromCode(Integer)}）、 未定義コードや null
 * 入力時のフォールバック（{@link StampType#UNKNOWN}）、 および各コード値・説明文字列の整合性を検証します。
 * </p>
 */
@DisplayName("StampType 単体テスト")
class StampTypeTest {

  @Nested
  @DisplayName("fromCode コード逆引きシナリオ")
  class FromCodeScenario {

    /**
     * 【テスト観点 / 責務】 正常系検証。HRMOS 仕様の有効な数値コード（1: 出勤, 2: 退勤, 7: 休憩開始, 8: 休憩終了, -1: 不明）が渡された際、 対応する正しい
     * {@link StampType} 定数が取得でき、各アクセサ（code, description）が正しく返却されることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>有効な数値コード（1, 2, 7, 8, -1）を指定</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>期待される Enum 定数が返却されること</li>
     *   <li>{@link StampType#getCode()} が一致すること</li>
     *   <li>{@link StampType#getDescription()} が一致すること</li>
     * </ul>
     *
     * @param code                入力数値コード
     * @param expectedName        期待される Enum 名
     * @param expectedDescription 期待される日本語説明
     */
    @ParameterizedTest
    @CsvSource({
        "1,  CLOCK_IN,    出勤",
        "2,  CLOCK_OUT,   退勤",
        "7,  BREAK_START, 休憩開始",
        "8,  BREAK_END,   休憩終了",
        "-1, UNKNOWN,     不明"
    })
    @DisplayName("正常系: 有効な数値コードから対応する StampType が正しく逆引きできること")
    void shouldResolveStampTypeFromValidCode(int code, String expectedName,
        String expectedDescription) {
      // [When]
      StampType actual = StampType.fromCode(code);

      // [Then]
      assertThat(actual.name()).isEqualTo(expectedName);
      assertThat(actual.getCode()).isEqualTo(code);
      assertThat(actual.getDescription()).isEqualTo(expectedDescription);
    }

    /**
     * 【テスト観点 / 責務】 境界値・C1 分岐検証。引数コードに {@code null} が渡された場合、 {@link NullPointerException} を発生させずに安全に
     * {@link StampType#UNKNOWN} が返却されることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>{@code code} に {@code null} を渡す</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>{@link StampType#UNKNOWN} が返却されること</li>
     * </ul>
     */
    @Test
    @DisplayName("境界値: code が null の場合、NullPointerException を起こさず UNKNOWN を返却すること")
    void shouldReturnUnknownWhenCodeIsNull() {
      // [When]
      StampType actual = StampType.fromCode(null);

      // [Then]
      assertThat(actual).isEqualTo(StampType.UNKNOWN);
    }

    /**
     * 【テスト観点 / 責務】 異常系・C1 分岐検証。Enum に定義されていない未知の数値コードが渡された場合、 フォールバックとして {@link StampType#UNKNOWN}
     * が返却されることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>未定義の数値コード（0, 3, 4, 99, 999, -99 等）を指定</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>{@link StampType#UNKNOWN} が返却されること</li>
     * </ul>
     *
     * @param undefinedCode 未定義の数値コード
     */
    @ParameterizedTest
    @ValueSource(ints = {0, 3, 4, 5, 6, 99, 999, -99})
    @DisplayName("異常系: 未定義の数値コードの場合、UNKNOWN を返却すること")
    void shouldReturnUnknownWhenCodeIsUndefined(int undefinedCode) {
      // [When]
      StampType actual = StampType.fromCode(undefinedCode);

      // [Then]
      assertThat(actual).isEqualTo(StampType.UNKNOWN);
    }
  }
}
