package com.computer_rescuer.attendance_management.shared;

import static com.computer_rescuer.attendance_management.shared.DateTimeConstants.JST;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 単体テスト環境における決定論的な日時シミュレーションを提供するテスト専用クロックファクトリ。
 * <p>
 * 日本標準時（JST / Asia/Tokyo）を基準タイムゾーンとして内包した {@link Clock} インスタンスを生成します。<br>
 * テストコード上で直感的な文字列表現（ISO-8601 形式の日付・時刻文字列）から即座に固定クロックを構築できるようにし、 単体テストの記述性、可読性、および保守性を最大化します。
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TestClockFactory {

  /**
   * テストで統一して使用する基準タイムゾーン（日本標準時: Asia/Tokyo）。
   */
  public static final ZoneId DEFAULT_ZONE_ID = JST;

  /**
   * 日付文字列と時刻文字列を指定して、指定日時に固定された {@link Clock} を生成します。
   * <p>
   * タイムゾーンは自動的に日本標準時（JST）が適用されます。
   * </p>
   *
   * @param dateString 日付文字列（形式: yyyy-MM-dd、例: "2026-04-20"）
   * @param timeString 時刻文字列（形式: HH:mm:ss または HH:mm、例: "10:00:00", "09:30"）
   * @return 指定された日時に固定された {@link Clock} インスタンス
   * @throws IllegalArgumentException 引数が null または空白文字の場合
   * @throws DateTimeParseException   日付または時刻のパースに失敗した場合
   */
  public static Clock fixedAt(String dateString, String timeString) {
    if (dateString == null || dateString.isBlank()) {
      throw new IllegalArgumentException("dateString は必須であり、空文字であってはなりません。");
    }
    if (timeString == null || timeString.isBlank()) {
      throw new IllegalArgumentException("timeString は必須であり、空文字であってはなりません。");
    }

    LocalDate date = LocalDate.parse(dateString);
    LocalTime time = LocalTime.parse(timeString);
    return fixedAt(date, time);
  }

  /**
   * ISO-8601 拡張形式の日時文字列を指定して、指定日時に固定された {@link Clock} を生成します。
   *
   * @param dateTimeString 日時文字列（形式: yyyy-MM-dd'T'HH:mm:ss、例: "2026-04-20T10:00:00"）
   * @return 指定された日時に固定された {@link Clock} インスタンス
   * @throws IllegalArgumentException 引数が null または空白文字の場合
   * @throws DateTimeParseException   日時のパースに失敗した場合
   */
  public static Clock fixedAt(String dateTimeString) {
    if (dateTimeString == null || dateTimeString.isBlank()) {
      throw new IllegalArgumentException("dateTimeString は必須であり、空文字であってはなりません。");
    }

    LocalDateTime dateTime = LocalDateTime.parse(dateTimeString);
    return fixedAt(dateTime);
  }

  /**
   * 日付文字列のみを指定し、当日の開始時刻（00:00:00 JST）に固定された {@link Clock} を生成します。
   *
   * @param dateString 日付文字列（形式: yyyy-MM-dd、例: "2026-04-20"）
   * @return 指定日の 00:00:00 に固定された {@link Clock} インスタンス
   * @throws IllegalArgumentException 引数が null または空白文字の場合
   * @throws DateTimeParseException   日付のパースに失敗した場合
   */
  public static Clock fixedAtDate(String dateString) {
    if (dateString == null || dateString.isBlank()) {
      throw new IllegalArgumentException("dateString は必須であり、空文字であってはなりません。");
    }

    LocalDate date = LocalDate.parse(dateString);
    return fixedAt(date, LocalTime.MIDNIGHT);
  }

  /**
   * 型安全な {@link LocalDate} と {@link LocalTime} から固定された {@link Clock} を生成します。
   *
   * @param date 対象日
   * @param time 対象時刻
   * @return 指定された日時に固定された {@link Clock} インスタンス
   */
  public static Clock fixedAt(LocalDate date, LocalTime time) {
    return fixedAt(LocalDateTime.of(date, time));
  }

  /**
   * 型安全な {@link LocalDateTime} から固定された {@link Clock} を生成します。
   *
   * @param dateTime 対象日時（日本標準時として解釈されます）
   * @return 指定された日時に固定された {@link Clock} インスタンス
   */
  public static Clock fixedAt(LocalDateTime dateTime) {
    Instant instant = dateTime.atZone(DEFAULT_ZONE_ID).toInstant();
    return Clock.fixed(instant, DEFAULT_ZONE_ID);
  }
}
