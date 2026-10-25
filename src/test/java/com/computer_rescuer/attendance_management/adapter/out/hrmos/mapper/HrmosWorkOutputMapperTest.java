package com.computer_rescuer.attendance_management.adapter.out.hrmos.mapper;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.computer_rescuer.attendance_management.adapter.out.hrmos.model.HrmosDailyWorkOutput;
import com.computer_rescuer.attendance_management.domain.model.DailyWorkRecord;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;

/**
 * {@link HrmosWorkOutputMapper} の単体テストクラス。
 * <p>
 * HRMOS 日次勤怠生データ（文字列主体）からドメイン実績モデルへの変換、 階層所属名の結合補正、および多様な時刻フォーマット（ISO-8601、4桁ゼロ埋め、パースエラー）の
 * 吸収ロジックにおける命令網羅（C0 100%）を検証します。
 * </p>
 */
@DisplayName("HrmosWorkOutputMapper 単体テスト")
class HrmosWorkOutputMapperTest {

  private HrmosWorkOutputMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = Mappers.getMapper(HrmosWorkOutputMapper.class);
  }

  @Nested
  @DisplayName("joinDepartments 部門階層結合シナリオ")
  class JoinDepartmentsScenario {

    @Test
    @DisplayName("正常系: 複数階層の部門名が「 ＞ 」記号で結合されること")
    void shouldJoinMultipleDepartmentNames() {
      // [Given]
      List<String> depts = List.of("開発本部", "サービス開発部", "勤怠チーム");

      // [When]
      String actual = mapper.joinDepartments(depts);

      // [Then]
      assertEquals("開発本部 ＞ サービス開発部 ＞ 勤怠チーム", actual);
    }

    @Test
    @DisplayName("単一要素: 階層が1つの場合、そのままの文字列が返却されること")
    void shouldReturnSingleDepartmentName() {
      assertEquals("営業部", mapper.joinDepartments(List.of("営業部")));
    }

    @Test
    @DisplayName("null 安全性: リストが null または空の場合、「未所属」が返却されること")
    void shouldReturnFallbackWhenDepartmentNamesIsNullOrEmpty() {
      assertAll(
          () -> assertEquals("未所属", mapper.joinDepartments(null)),
          () -> assertEquals("未所属", mapper.joinDepartments(Collections.emptyList()))
      );
    }
  }

  @Nested
  @DisplayName("parseDateString 日付パースシナリオ")
  class ParseDateStringScenario {

    @Test
    @DisplayName("正常系: yyyy-MM-dd 形式の日付文字列が LocalDate へパースされること")
    void shouldParseValidDateString() {
      assertEquals(LocalDate.of(2026, 4, 20), mapper.parseDateString("2026-04-20"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("境界値: 日付文字列が null または空白の場合、null を返却すること")
    void shouldReturnNullWhenDateStringIsBlank(String blankStr) {
      assertNull(mapper.parseDateString(blankStr));
    }

    @Test
    @DisplayName("異常系: 不正な日付形式の場合、例外をスローせず null を返却すること")
    void shouldReturnNullWhenDateFormatIsInvalid() {
      assertNull(mapper.parseDateString("2026/04/20"));
    }
  }

  @Nested
  @DisplayName("parseTimeString 時刻パースシナリオ")
  class ParseTimeStringScenario {

    @Test
    @DisplayName("正常系 (ISO形式): T を含む OffsetDateTime 文字列から日本時間の LocalTime が抽出されること")
    void shouldParseIsoOffsetDateTimeString() {
      // UTC 00:00:00 -> JST 09:00:00
      LocalTime actual = mapper.parseTimeString("2026-04-20T00:00:00Z");
      assertEquals(LocalTime.of(9, 0, 0), actual);
    }

    @Test
    @DisplayName("正常系 (4桁補正): 「9:00」のような4桁文字列が「09:00」にゼロ埋め補正されてパースされること")
    void shouldPadZeroWhenTimeLengthIsFour() {
      LocalTime actual = mapper.parseTimeString("9:00");
      assertEquals(LocalTime.of(9, 0), actual, "0埋め補正されて 09:00 としてパースされること");
    }

    @Test
    @DisplayName("正常系 (5桁標準): 「18:30」のような標準時刻文字列が正しくパースされること")
    void shouldParseStandardFiveDigitTime() {
      LocalTime actual = mapper.parseTimeString("18:30");
      assertEquals(LocalTime.of(18, 30), actual);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("境界値: 時刻文字列が null または空白の場合、null を返却すること")
    void shouldReturnNullWhenTimeStringIsBlank(String blankStr) {
      assertNull(mapper.parseTimeString(blankStr));
    }

    @Test
    @DisplayName("異常系: 不正な時刻形式の場合、例外をスローせず null を返却すること")
    void shouldReturnNullWhenTimeFormatIsInvalid() {
      assertNull(mapper.parseTimeString("25:99"));
    }
  }

  @Nested
  @DisplayName("toDomain および toDomainList の統合検証")
  class ToDomainScenario {

    /**
     * 生の日次勤怠モデルから DailyWorkRecord への項目マッピングが正常に行われることを検証します。
     */
    @Test
    @DisplayName("正常系: HrmosDailyWorkOutput から DailyWorkRecord への完全マッピングが行われること")
    void shouldMapToDailyWorkRecord() {
      // [Given]
      HrmosDailyWorkOutput raw = new HrmosDailyWorkOutput(
          101, "EMP001", "徳川 家康", "202604", "2026-04-20", "月",
          "通常", "通常勤務", 1, "09:00", 0, "08:50",
          List.of("開発本部", "システム部")
      );

      // [When]
      DailyWorkRecord record = mapper.toDomain(raw);
      List<DailyWorkRecord> list = mapper.toDomainList(List.of(raw));

      // [Then]
      assertAll(
          () -> assertEquals(101, record.userId()),
          () -> assertEquals("EMP001", record.employeeNumber()),
          () -> assertEquals("徳川 家康", record.fullName()),
          () -> assertEquals("開発本部 ＞ システム部", record.departmentName()),
          () -> assertEquals(LocalDate.of(2026, 4, 20), record.date()),
          () -> assertEquals("通常勤務", record.segmentTitle()),
          () -> assertEquals(1, record.applicationStatus()),
          () -> assertEquals(LocalTime.of(9, 0), record.actualStartTime()),
          () -> assertEquals(0, record.nextDayStart()),
          () -> assertEquals(LocalTime.of(8, 50), record.stampingTime()),
          () -> assertEquals(1, list.size())
      );
    }
  }
}
