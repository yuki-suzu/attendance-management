package com.computer_rescuer.attendance_management.adapter.out.hrmos.mapper;

import static com.computer_rescuer.attendance_management.shared.DateTimeConstants.JST;

import com.computer_rescuer.attendance_management.adapter.out.hrmos.model.HrmosStampLog;
import com.computer_rescuer.attendance_management.domain.model.Employee;
import com.computer_rescuer.attendance_management.domain.model.StampLog;
import com.computer_rescuer.attendance_management.domain.model.StampType;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import org.springframework.util.StringUtils;

/**
 * HRMOSの打刻ログ（生データ）の変換・加工を担う統合マッパー。
 * <p>
 * HRMOS APIから取得した打刻データと、ローカルDBの従業員情報を結合し、 完全なドメインモデル（StampLog）を生成します。
 * </p>
 */
@Slf4j
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class HrmosStampLogMapper {

  /**
   * MapStructの自動生成に頼らず、Stream API を使って自力でリストを変換する。 これにより、MapStructが「どの Employee
   * を渡せばいいのか」で迷うことがなくなります。
   *
   * @param rawList     生の打刻ログリスト
   * @param employeeMap userIdをキーとした社員情報のMap
   * @return 結合された打刻ログ(ドメインモデル)のリスト
   */
  public List<StampLog> toDomainList(List<HrmosStampLog> rawList,
      Map<Integer, Employee> employeeMap) {
    if (rawList == null || rawList.isEmpty()) {
      return List.of();
    }
    // 💡 Map の null チェックも念のため実施（NullPointerException防止）
    Map<Integer, Employee> safeMap = (employeeMap != null) ? employeeMap : Map.of();

    return rawList.stream()
        .map(raw -> {
          // 生データのuserIdから、対象の社員情報を取得（なければnullを渡す）
          Employee employee = safeMap.get(raw.userId());
          return toDomain(raw, employee);
        })
        .toList();
  }

  /**
   * HRMOSの生データと、ローカルDBの社員情報を結合してドメインモデルに変換します。
   *
   * @param raw      HRMOSAPIから取得した生の打刻ログ
   * @param employee ローカルDBから取得した社員情報（存在しない場合はnullが渡されます）
   * @return 結合された打刻ログドメインモデル
   */
  @Mapping(source = "raw.userId", target = "userId")
  @Mapping(source = "raw.createdAt", target = "stampingAt", qualifiedByName = "parseDateTimeString")
  @Mapping(source = "raw.stampType", target = "stampType", qualifiedByName = "toStampTypeEnum")
  @Mapping(source = "employee.employeeNumber", target = "employeeNumber")
  @Mapping(source = "employee.lastName", target = "lastName")
  @Mapping(source = "employee.firstName", target = "firstName")
  // userAgent は raw から自動マッピングされます
  public abstract StampLog toDomain(HrmosStampLog raw, Employee employee);

  /**
   * ISO-8601拡張形式の文字列を LocalDateTime (日本時間) に安全に変換します。
   */
  @Named("parseDateTimeString")
  protected LocalDateTime parseDateTimeString(String dateTimeStr) {
    if (!StringUtils.hasText(dateTimeStr)) {
      return null;
    }
    try {
      return OffsetDateTime.parse(dateTimeStr).atZoneSameInstant(JST).toLocalDateTime();
    } catch (DateTimeParseException e) {
      log.warn("打刻日時のパースに失敗しました: {}", dateTimeStr);
      return null;
    }
  }

  /**
   * 数値コードをドメインの Enum 型に変換します。
   */
  @Named("toStampTypeEnum")
  protected StampType toStampTypeEnum(Integer code) {
    return StampType.fromCode(code);
  }

  /**
   * 打刻ログ（ドメインモデル）のリストから、ユーザーIDをキーとした「本日の出勤打刻時刻」のマップを作成します。
   *
   * @param stamps ドメインモデルに変換済みの打刻ログリスト
   * @return HRMOSのユーザーID(Integer)と出勤時刻(LocalTime)のMap
   */
  public Map<Integer, LocalTime> toClockInMap(List<StampLog> stamps) {
    if (stamps == null || stamps.isEmpty()) {
      return Map.of();
    }

    return stamps.stream()
        .filter(StampLog::isClockIn)
        .collect(Collectors.toMap(
            StampLog::userId,
            log -> log.stampingAt().toLocalTime(),
            (existing, replacement) -> existing.isBefore(replacement) ? existing : replacement
        ));
  }
}
