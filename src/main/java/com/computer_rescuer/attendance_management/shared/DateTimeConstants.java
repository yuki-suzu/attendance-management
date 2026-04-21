package com.computer_rescuer.attendance_management.shared;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * プロジェクト全体で共通して使用する、日付・時刻関連のフォーマットおよび定数群。
 * <p>
 * アプリケーション内でタイムゾーンやフォーマットの揺らぎを防ぐため、 日時操作を行う際はこのクラスの定数を必ず使用してください。
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE) // インスタンス化を防ぐ
public final class DateTimeConstants {

  /**
   * システムの標準タイムゾーン（日本時間: Asia/Tokyo）
   */
  public static final ZoneId JST = ZoneId.of("Asia/Tokyo");

  /**
   * 標準的な日付フォーマット (ISO-8601 拡張形式)
   * <p>例: "2026-04-18"</p>
   */
  public static final DateTimeFormatter ISO_LOCAL_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

  /**
   * オフセット付きの日時フォーマット (ISO-8601 拡張形式)
   * <p>例: "2026-04-18T15:30:00+09:00"</p>
   * <p>HRMOS API などの外部連携で標準的に使用されます。</p>
   */
  public static final DateTimeFormatter ISO_OFFSET_DATE_TIME = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

  /**
   * 標準的な時刻フォーマット
   * <p>例: "15:30:00"</p>
   */
  public static final DateTimeFormatter ISO_LOCAL_TIME = DateTimeFormatter.ISO_LOCAL_TIME;

}
