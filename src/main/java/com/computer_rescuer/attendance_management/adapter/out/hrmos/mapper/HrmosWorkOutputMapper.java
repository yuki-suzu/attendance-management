package com.computer_rescuer.attendance_management.adapter.out.hrmos.mapper;

import com.computer_rescuer.attendance_management.adapter.out.hrmos.model.HrmosDailyWorkOutput;
import com.computer_rescuer.attendance_management.domain.model.DailyWorkRecord;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/**
 * HRMOSの日次勤怠レスポンス（文字列主体）を、システム内で扱う純粋な実績データ（DailyWorkRecord）へ変換するマッパー。
 * <p>
 * 外部API特有の揺らぎ（日付・時刻のパースエラー等）を安全に吸収するため、 カスタムマッピングメソッド（@Named）を実装しています。
 * パースエラー時の警告はLombokの@Slf4jを利用して出力します。
 * </p>
 */
@Slf4j
@Mapper(componentModel = "spring")
public abstract class HrmosWorkOutputMapper {

  private static final ZoneId JST = ZoneId.of("Asia/Tokyo");

  /**
   * HRMOSの勤怠生データ（文字列）を、ドメイン層の純粋な勤怠実績データへ変換します。
   *
   * @param raw 変換元のHRMOS日次勤怠生データ
   * @return 変換後の勤怠実績データ。パースに失敗した日時項目は null となります。
   */
  @Mapping(source = "number", target = "employeeNumber")
  @Mapping(source = "day", target = "date", qualifiedByName = "parseDateString")
  @Mapping(source = "status", target = "applicationStatus")
  @Mapping(source = "startAt", target = "actualStartTime", qualifiedByName = "parseTimeString")
  @Mapping(source = "stampingStartAt", target = "stampingTime", qualifiedByName = "parseTimeString")
  public abstract DailyWorkRecord toDomain(HrmosDailyWorkOutput raw);

  /**
   * HRMOSの勤怠生データのリストを、勤怠実績データのリストへ一括変換します。
   */
  public abstract List<DailyWorkRecord> toDomainList(List<HrmosDailyWorkOutput> rawList);

  /**
   * 文字列の日付を {@link LocalDate} に安全に変換します。
   */
  @Named("parseDateString")
  protected LocalDate parseDateString(String dateStr) {
    if (dateStr == null || dateStr.isBlank()) {
      return null;
    }
    try {
      return LocalDate.parse(dateStr);
    } catch (DateTimeParseException e) {
      log.warn("日付のパースに失敗しました: {}", dateStr);
      return null;
    }
  }

  /**
   * 文字列の時刻を {@link LocalTime} に安全に変換します。
   * <p>
   * ISO-8601拡張形式（OffsetDateTime）と、単純な時刻文字列（"09:00", "9:00"）の両方に対応します。
   * </p>
   */
  @Named("parseTimeString")
  protected LocalTime parseTimeString(String timeStr) {
    if (timeStr == null || timeStr.isBlank()) {
      return null;
    }
    try {
      if (timeStr.contains("T")) {
        return OffsetDateTime.parse(timeStr).atZoneSameInstant(JST).toLocalTime();
      }
      String padded = timeStr.length() == 4 ? "0" + timeStr : timeStr;
      return LocalTime.parse(padded);
    } catch (DateTimeParseException e) {
      log.warn("時刻のパースに失敗しました: {}", timeStr);
      return null;
    }
  }
}
