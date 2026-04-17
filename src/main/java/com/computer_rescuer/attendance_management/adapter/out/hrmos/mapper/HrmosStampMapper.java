package com.computer_rescuer.attendance_management.adapter.out.hrmos.mapper;

import com.computer_rescuer.attendance_management.adapter.out.hrmos.model.HrmosStampLog;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.Mapper;

/**
 * HRMOSの打刻ログデータを、システム内部で扱いやすい形式に変換・加工するマッパー。
 */
@Slf4j
@Mapper(componentModel = "spring")
public abstract class HrmosStampMapper {

  private static final ZoneId JST = ZoneId.of("Asia/Tokyo");

  /**
   * 打刻ログのリストから、ユーザーIDをキーとした「本日の出勤打刻時刻」のマップを作成します。
   * <p>
   * 同一ユーザーに複数の打刻がある場合、最も早い時刻を採用します。
   * </p>
   *
   * @param stamps APIから取得した生の打刻ログリスト
   * @return ユーザーIDと出勤時刻のMap
   */
  public Map<Integer, LocalTime> toClockInMap(List<HrmosStampLog> stamps) {
    if (stamps == null || stamps.isEmpty()) {
      return Map.of();
    }

    return stamps.stream()
        .filter(HrmosStampLog::isClockIn)
        .collect(Collectors.toMap(
            HrmosStampLog::userId,
            s -> parseStampingTime(s.createdAt()),
            // 同一ユーザーが複数回出勤打刻した場合は、一番古い時刻を採用する
            (existing, replacement) -> existing.isBefore(replacement) ? existing : replacement
        ));
  }

  /**
   * 文字列形式の打刻時刻を LocalTime に安全に変換します。
   *
   * @param timeStr 打刻時刻文字列
   * @return 変換後の時刻。パース失敗時は null
   */
  protected LocalTime parseStampingTime(String timeStr) {
    if (timeStr == null || timeStr.isBlank()) {
      return null;
    }
    try {
      if (timeStr.contains("T")) {
        return OffsetDateTime.parse(timeStr).atZoneSameInstant(JST).toLocalTime();
      }
      return LocalTime.parse(timeStr.length() == 4 ? "0" + timeStr : timeStr);
    } catch (Exception e) {
      log.warn("打刻時刻のパースに失敗しました: {}", timeStr);
      return null;
    }
  }
}
