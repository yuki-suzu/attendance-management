package com.computer_rescuer.attendance_management.adapter.out.hrmos.mapper;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.computer_rescuer.attendance_management.adapter.out.hrmos.model.HrmosSegment;
import com.computer_rescuer.attendance_management.domain.model.Segment;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

/**
 * {@link HrmosSegmentMapper} の単体テストクラス。
 * <p>
 * HRMOS 勤務区分モデルからドメインモデルへの変換、および default メソッドによる OffsetDateTime から日本時間（JST）LocalTime への抽出・補正ロジックの
 * 命令網羅（C0 100%）を検証します。 Spring コンテキストを起動せず、MapStruct のファクトリを用いて高速に実行します。
 * </p>
 */
@DisplayName("HrmosSegmentMapper 単体テスト")
class HrmosSegmentMapperTest {

  private HrmosSegmentMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = Mappers.getMapper(HrmosSegmentMapper.class);
  }

  @Nested
  @DisplayName("map(OffsetDateTime) default メソッドの検証")
  class MapOffsetDateTimeScenario {

    /**
     * 引数が null の場合、安全に null が返却されることを検証します（C0 分岐網羅）。
     */
    @Test
    @DisplayName("null 安全性: OffsetDateTime が null の場合、null を返却すること")
    void shouldReturnNullWhenOffsetDateTimeIsNull() {
      // [When]
      LocalTime actual = mapper.map((OffsetDateTime) null);

      // [Then]
      assertNull(actual, "null がそのまま返却されること");
    }

    /**
     * UTC（+00:00）で表現された日時が渡された場合、 日本時間（+09:00）に正しく変換された針の時刻（LocalTime）が抽出されることを検証します。
     */
    @Test
    @DisplayName("タイムゾーン補正: UTC の日時が渡された場合、JST（+9時間）換算の LocalTime が返却されること")
    void shouldExtractLocalTimeInJstWhenUtcProvided() {
      // [Given] UTC 00:00:00 -> JST 09:00:00
      OffsetDateTime utcDateTime = OffsetDateTime.parse("2026-04-20T00:00:00Z");

      // [When]
      LocalTime actual = mapper.map(utcDateTime);

      // [Then]
      assertEquals(LocalTime.of(9, 0, 0), actual, "日本時間の午前9時として抽出されること");
    }
  }

  @Nested
  @DisplayName("toDomain および toDomainList の検証")
  class ToDomainScenario {

    /**
     * 単一モデルおよびリストモデルの相互マッピングが正常に行われることを検証します。
     */
    @Test
    @DisplayName("正常系: HrmosSegment から Segment ドメインモデルへ正しく変換されること")
    void shouldMapToDomainAndDomainList() {
      // [Given]
      OffsetDateTime startAt = OffsetDateTime.parse("2026-04-20T09:00:00+09:00");
      OffsetDateTime endAt = OffsetDateTime.parse("2026-04-20T18:00:00+09:00");
      HrmosSegment raw = new HrmosSegment(1, "出勤", "通常出勤", 1, startAt, endAt);

      // [When]
      Segment domain = mapper.toDomain(raw);
      List<Segment> domainList = mapper.toDomainList(List.of(raw));

      // [Then]
      assertAll(
          () -> assertEquals(1, domain.id()),
          () -> assertEquals("出勤", domain.title()),
          () -> assertEquals("通常出勤", domain.displayTitle()),
          () -> assertEquals(1, domain.status()),
          () -> assertEquals(LocalTime.of(9, 0), domain.startAt()),
          () -> assertEquals(LocalTime.of(18, 0), domain.endAt()),
          () -> assertEquals(1, domainList.size())
      );
    }
  }
}
