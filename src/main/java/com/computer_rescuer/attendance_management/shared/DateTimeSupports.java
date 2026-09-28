package com.computer_rescuer.attendance_management.shared;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * プロジェクト全体で共通して使用する、日付・時刻関連のフォーマット、定数、および補助操作ユーティリティ。
 * <p>
 * アプリケーション内でタイムゾーンやフォーマットの揺らぎを防ぐため、 日時操作および外部 API 向けの日時文字列変換を行う際はこのクラスの機能を使用してください。
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DateTimeSupports {

  /**
   * システムの標準タイムゾーン（日本標準時: Asia/Tokyo）。
   */
  public static final ZoneId JST = ZoneId.of("Asia/Tokyo");

  /**
   * 標準的な日付フォーマット（ISO-8601 拡張形式: yyyy-MM-dd）。
   * <p>例: "2026-04-18"</p>
   */
  public static final DateTimeFormatter ISO_LOCAL_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

  /**
   * オフセット付きの日時フォーマット（ISO-8601 拡張形式）。
   * <p>例: "2026-04-18T15:30:00+09:00"</p>
   * <p>HRMOS API などの外部連携で標準的に使用されます。</p>
   */
  public static final DateTimeFormatter ISO_OFFSET_DATE_TIME = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

  /**
   * 標準的な時刻フォーマット（HH:mm:ss）。
   * <p>例: "15:30:00"</p>
   */
  public static final DateTimeFormatter ISO_LOCAL_TIME = DateTimeFormatter.ISO_LOCAL_TIME;

  /**
   * 期間検索（未満判定）において指定日当日を完全に含めるため、翌日の日付を取得します。
   *
   * @param date 基準日
   * @return 翌日の日付（引数が null の場合は null）
   */
  public static LocalDate toEndOfDay(LocalDate date) {
    if (date == null) {
      return null;
    }
    return date.plusDays(1);
  }

  /**
   * {@link LocalDate} を日本標準時（JST）の開始時刻（00:00:00）として、 ISO-8601 オフセット付き日時文字列へ変換します。
   *
   * @param date 変換対象の日付
   * @return ISO-8601 形式の日時文字列（例: "2026-04-01T00:00:00+09:00"）。引数が null の場合は null
   */
  public static String localDateToIsoDateTimeString(LocalDate date) {
    if (date == null) {
      return null;
    }
    return date.atStartOfDay(JST).format(ISO_OFFSET_DATE_TIME);
  }
}
