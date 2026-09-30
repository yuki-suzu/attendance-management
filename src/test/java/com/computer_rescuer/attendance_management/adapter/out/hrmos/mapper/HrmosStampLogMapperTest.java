package com.computer_rescuer.attendance_management.adapter.out.hrmos.mapper;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.computer_rescuer.attendance_management.adapter.out.hrmos.model.HrmosStampLog;
import com.computer_rescuer.attendance_management.domain.model.Employee;
import com.computer_rescuer.attendance_management.domain.model.StampLog;
import com.computer_rescuer.attendance_management.domain.model.StampType;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;

/**
 * {@link HrmosStampLogMapper} の単体テストクラス。
 * <p>
 * 生打刻ログとローカル社員情報の結合エンリッチ処理、ISO-8601 文字列パースの例外ハンドリング、 および同日内重複打刻時における最短出勤時刻採用（{@code isBefore}
 * 判定）の業務ルールの 命令網羅（C0 100%）を検証します。
 * </p>
 */
@DisplayName("HrmosStampLogMapper 単体テスト")
class HrmosStampLogMapperTest {

  private HrmosStampLogMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = Mappers.getMapper(HrmosStampLogMapper.class);
  }

  @Nested
  @DisplayName("toDomainList エンリッチ結合シナリオ")
  class ToDomainListScenario {

    @Test
    @DisplayName("正常系: 生打刻ログに対応する Employee 情報がマージされて完全なドメインモデルが生成されること")
    void shouldEnrichStampLogsWithEmployeeData() {
      // [Given]
      HrmosStampLog raw = new HrmosStampLog(
          101, "2026-04-20T08:55:00+09:00", 1, 1, 1, "curl/7.68.0");
      Employee employee = new Employee(
          101, "EMP001", "山田", "太郎", "yamada@example.com", 1, 1, 1);
      Map<Integer, Employee> employeeMap = Map.of(101, employee);

      // [When]
      List<StampLog> result = mapper.toDomainList(List.of(raw), employeeMap);

      // [Then]
      assertAll(
          () -> assertEquals(1, result.size()),
          () -> assertEquals(101, result.get(0).userId()),
          () -> assertEquals("EMP001", result.get(0).employeeNumber()),
          () -> assertEquals("山田", result.get(0).lastName()),
          () -> assertEquals("太郎", result.get(0).firstName()),
          () -> assertEquals(StampType.CLOCK_IN, result.get(0).stampType()),
          () -> assertEquals(LocalDateTime.of(2026, 4, 20, 8, 55, 0), result.get(0).stampingAt()),
          () -> assertEquals("curl/7.68.0", result.get(0).userAgent())
      );
    }

    @Test
    @DisplayName("正常系: employeeMap に対象社員が存在しない場合、社員名フィールドが null で生成されること")
    void shouldHandleMissingEmployeeInMap() {
      // [Given]
      HrmosStampLog raw = new HrmosStampLog(
          999, "2026-04-20T08:55:00+09:00", 1, 1, 1, "Safari");

      // [When]
      List<StampLog> result = mapper.toDomainList(List.of(raw), Collections.emptyMap());

      // [Then]
      assertAll(
          () -> assertEquals(1, result.size()),
          () -> assertEquals(999, result.get(0).userId()),
          () -> assertNull(result.get(0).employeeNumber(), "社員番号は null であること"),
          () -> assertNull(result.get(0).lastName(), "姓は null であること")
      );
    }

    @Test
    @DisplayName("null 安全性: rawList が null または空の場合、空リストを返却すること")
    void shouldReturnEmptyListWhenRawListIsNullOrEmpty() {
      assertAll(
          () -> assertTrue(mapper.toDomainList(null, Collections.emptyMap()).isEmpty()),
          () -> assertTrue(
              mapper.toDomainList(Collections.emptyList(), Collections.emptyMap()).isEmpty())
      );
    }

    @Test
    @DisplayName("null 安全性: employeeMap が null の場合でも NullPointerException を防ぎ安全に処理されること")
    void shouldHandleNullEmployeeMapSafely() {
      // [Given]
      HrmosStampLog raw = new HrmosStampLog(
          101, "2026-04-20T08:55:00+09:00", 1, 1, 1, "Agent");

      // [When]
      List<StampLog> result = mapper.toDomainList(List.of(raw), null);

      // [Then]
      assertEquals(1, result.size());
      assertNull(result.get(0).employeeNumber());
    }
  }

  @Nested
  @DisplayName("parseDateTimeString 日時パースシナリオ")
  class ParseDateTimeStringScenario {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("境界値: 日時文字列が null または空白の場合、null を返却すること")
    void shouldReturnNullWhenDateTimeStringIsBlank(String blankStr) {
      assertNull(mapper.parseDateTimeString(blankStr));
    }

    @Test
    @DisplayName("異常系: 不正な日付フォーマットの場合、例外をスローせず null を返却すること")
    void shouldReturnNullWhenDateTimeParseExceptionOccurs() {
      assertNull(mapper.parseDateTimeString("invalid-date-format"));
    }
  }

  @Nested
  @DisplayName("toStampTypeEnum 変換シナリオ")
  class ToStampTypeEnumScenario {

    @Test
    @DisplayName("正常系: 数値コードから正しい StampType Enum が導出されること")
    void shouldConvertCodeToStampType() {
      assertAll(
          () -> assertEquals(StampType.CLOCK_IN, mapper.toStampTypeEnum(1)),
          () -> assertEquals(StampType.CLOCK_OUT, mapper.toStampTypeEnum(2)),
          () -> assertEquals(StampType.UNKNOWN, mapper.toStampTypeEnum(999)),
          () -> assertEquals(StampType.UNKNOWN, mapper.toStampTypeEnum(null))
      );
    }
  }

  @Nested
  @DisplayName("toClockInMap 出勤打刻マップ生成シナリオ")
  class ToClockInMapScenario {

    /**
     * 出勤打刻（CLOCK_IN）のみが抽出され、退勤打刻は除外されることを検証します。
     */
    @Test
    @DisplayName("正常系: 退勤打刻を除外し、出勤打刻のみを対象として Map が作成されること")
    void shouldFilterOnlyClockInStamps() {
      // [Given]
      StampLog clockIn = new StampLog(
          101, "EMP001", "山田", "太郎",
          LocalDateTime.of(2026, 4, 20, 8, 45), StampType.CLOCK_IN, "Agent");
      StampLog clockOut = new StampLog(
          101, "EMP001", "山田", "太郎",
          LocalDateTime.of(2026, 4, 20, 18, 0), StampType.CLOCK_OUT, "Agent");

      // [When]
      Map<Integer, LocalTime> map = mapper.toClockInMap(List.of(clockIn, clockOut));

      // [Then]
      assertAll(
          () -> assertEquals(1, map.size()),
          () -> assertEquals(LocalTime.of(8, 45), map.get(101))
      );
    }

    /**
     * 同一ユーザーが同日に複数回出勤打刻を行った場合、 最も早い打刻時刻が採用される（isBefore の両ルート検証）ことを検証します。
     */
    @Test
    @DisplayName("重複排除ルール: 同一ユーザーの複数出勤打刻が存在する場合、最も早い時刻が採用されること")
    void shouldSelectEarliestClockInTimeWhenDuplicateExists() {
      // [Given] 早い打刻（8:30）と 遅い再打刻（8:50）
      StampLog earlyStamp = new StampLog(
          101, "EMP001", "山田", "太郎",
          LocalDateTime.of(2026, 4, 20, 8, 30), StampType.CLOCK_IN, "Agent");
      StampLog lateStamp = new StampLog(
          101, "EMP001", "山田", "太郎",
          LocalDateTime.of(2026, 4, 20, 8, 50), StampType.CLOCK_IN, "Agent");

      // [When] 順序を入れ替えて両方の比較パス（existing が早い場合 / replacement が早い場合）をテスト
      Map<Integer, LocalTime> map1 = mapper.toClockInMap(List.of(earlyStamp, lateStamp));
      Map<Integer, LocalTime> map2 = mapper.toClockInMap(List.of(lateStamp, earlyStamp));

      // [Then]
      assertAll(
          () -> assertEquals(LocalTime.of(8, 30), map1.get(101), "早い時刻（8:30）が維持されること"),
          () -> assertEquals(LocalTime.of(8, 30), map2.get(101),
              "後から早い時刻が来ても正しく置換されること")
      );
    }

    @Test
    @DisplayName("null 安全性: リストが null または空の場合、空マップを返却すること")
    void shouldReturnEmptyMapWhenStampsIsNullOrEmpty() {
      assertAll(
          () -> assertTrue(mapper.toClockInMap(null).isEmpty()),
          () -> assertTrue(mapper.toClockInMap(Collections.emptyList()).isEmpty())
      );
    }
  }
}
