package com.computer_rescuer.attendance_management.domain.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.computer_rescuer.attendance_management.domain.model.DailyAttendance;
import com.computer_rescuer.attendance_management.domain.model.DailyAttendance.Status;
import com.computer_rescuer.attendance_management.domain.model.DailyWorkRecord;
import com.computer_rescuer.attendance_management.domain.model.Segment;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * {@link DailyAttendanceFactory} の単体テストクラス。
 * <p>
 * 勤務区分、打刻時刻、所属部門情報の補完とマスタ突き合わせによる 出勤判定モデル（{@link DailyAttendance}）の構築ロジックを検証します。
 * </p>
 */
@DisplayName("DailyAttendanceFactory 単体テスト")
class DailyAttendanceFactoryTest {

  private DailyAttendanceFactory factory;

  private final LocalDate targetDate = LocalDate.of(2026, 4, 1);
  private final ZonedDateTime now = ZonedDateTime.parse("2026-04-01T10:00:00+09:00[Asia/Tokyo]");

  @BeforeEach
  void setUp() {
    factory = new DailyAttendanceFactory();
  }

  @Nested
  @DisplayName("エンリッチ処理とモデル生成の検証")
  class CreationScenario {

    /**
     * 所属部門が存在し、かつ勤務区分マスタが合致する場合に、 正しい情報でエンリッチされてドメインモデルが生成されることを検証します。
     */
    @Test
    @DisplayName("部門名および勤務区分が存在する場合、正しく値が補完されてインスタンスが生成されること")
    void shouldCreateDailyAttendanceWithEnrichedData() {
      // [Given]
      DailyWorkRecord record = mock(DailyWorkRecord.class);
      DailyWorkRecord enrichedRecord = mock(DailyWorkRecord.class);

      given(record.userId()).willReturn(101);
      given(record.segmentTitle()).willReturn("通常勤務");

      LocalTime stampingTime = LocalTime.of(8, 50);
      given(record.withStampingTime(stampingTime)).willReturn(enrichedRecord);
      given(enrichedRecord.withDepartmentName("システム開発部")).willReturn(enrichedRecord);

      // モデル内部のアクセサ戻り値設定
      given(enrichedRecord.userId()).willReturn(101);
      given(enrichedRecord.employeeNumber()).willReturn("EMP101");
      given(enrichedRecord.fullName()).willReturn("山田 太郎");
      given(enrichedRecord.departmentName()).willReturn("システム開発部");
      given(enrichedRecord.segmentTitle()).willReturn("通常勤務");
      given(enrichedRecord.actualStartTime()).willReturn(LocalTime.of(8, 50));
      given(enrichedRecord.stampingTime()).willReturn(stampingTime);

      Segment segment = mock(Segment.class);
      given(segment.startAt()).willReturn(LocalTime.of(9, 0));

      Map<String, Segment> segmentMap = Map.of("通常勤務", segment);
      Map<Integer, String> departmentMap = Map.of(101, "システム開発部");
      Map<Integer, LocalTime> clockInMap = Map.of(101, stampingTime);

      // [When]
      DailyAttendance actual = factory.create(
          record, segmentMap, departmentMap, clockInMap, targetDate, now);

      // [Then]
      assertAll(
          () -> assertEquals(101, actual.userId(), "従業員IDが一致すること"),
          () -> assertEquals("EMP101", actual.employeeNumber(), "社員番号が一致すること"),
          () -> assertEquals("山田 太郎", actual.fullName(), "氏名が一致すること"),
          () -> assertEquals("システム開発部", actual.departmentName(),
              "部門名が設定されていること"),
          () -> assertEquals("通常勤務", actual.segmentTitle(), "勤務区分名が一致すること"),
          () -> assertEquals(LocalTime.of(9, 0), actual.scheduledStartAt(),
              "予定出勤時刻が設定されていること"),
          () -> assertEquals(stampingTime, actual.stampingAt(), "打刻時刻が設定されていること"),
          () -> assertEquals(Status.ATTENDED, actual.status(),
              "打刻済みのためステータスがATTENDEDであること")
      );
    }

    /**
     * 部門マップに未登録、かつ勤務区分マスタに該当が存在しない場合に、 部門名が「未所属」となり、予定時刻が null として安全にフォールバックされることを検証します。
     */
    @Test
    @DisplayName("部門未登録かつ勤務区分が存在しない場合、未所属および予定時刻nullで生成されること")
    void shouldFallbackWhenDepartmentAndSegmentAreMissing() {
      // [Given]
      DailyWorkRecord record = mock(DailyWorkRecord.class);
      DailyWorkRecord enrichedRecord = mock(DailyWorkRecord.class);

      given(record.userId()).willReturn(999);
      given(record.segmentTitle()).willReturn("不明な区分");

      given(record.withStampingTime(null)).willReturn(enrichedRecord);
      given(enrichedRecord.withDepartmentName("未所属")).willReturn(enrichedRecord);

      given(enrichedRecord.userId()).willReturn(999);
      given(enrichedRecord.employeeNumber()).willReturn("EMP999");
      given(enrichedRecord.fullName()).willReturn("名無 権兵衛");
      given(enrichedRecord.departmentName()).willReturn("未所属");
      given(enrichedRecord.segmentTitle()).willReturn("不明な区分");
      given(enrichedRecord.actualStartTime()).willReturn(null);
      given(enrichedRecord.stampingTime()).willReturn(null);

      // [When]
      DailyAttendance actual = factory.create(
          record, Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(),
          targetDate, now);

      // [Then]
      assertAll(
          () -> assertEquals("未所属", actual.departmentName(),
              "部門名がデフォルト値の未所属になること"),
          () -> assertNull(actual.scheduledStartAt(),
              "存在しない勤務区分のため予定時刻がnullになること"),
          () -> assertEquals(Status.NOT_ATTENDED, actual.status(),
              "打刻がなく予定時刻も未設定のためNOT_ATTENDEDであること")
      );
    }
  }
}
