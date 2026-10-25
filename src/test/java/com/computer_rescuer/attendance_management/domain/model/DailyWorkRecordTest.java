package com.computer_rescuer.attendance_management.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * {@link DailyWorkRecord} の単体テストクラス。
 * <p>
 * 不変オブジェクト（イミュータブル）としてのデータ整合性と、 Wither パターン（{@link DailyWorkRecord#withStampingTime(LocalTime)},
 * {@link DailyWorkRecord#withDepartmentName(String)}）における null 安全性および新インスタンス生成ロジックを検証します。
 * </p>
 */
@DisplayName("DailyWorkRecord 単体テスト")
class DailyWorkRecordTest {

  private final DailyWorkRecord baseRecord = new DailyWorkRecord(
      101,
      "EMP001",
      "山田 太郎",
      "システム開発部",
      LocalDate.of(2026, 4, 20),
      "通常勤務",
      1,
      LocalTime.of(9, 0),
      0,
      LocalTime.of(8, 55)
  );

  @Nested
  @DisplayName("withStampingTime 打刻時刻更新シナリオ")
  class WithStampingTimeScenario {

    /**
     * 【テスト観点 / 責務】 正常系検証。非 null の打刻時刻を指定した際、元のオブジェクトを改変せず（イミュータブル）、
     * 指定した打刻時刻のみが更新された新しいインスタンスが返却され、他属性が維持されることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>更新用の打刻時刻として {@code 08:30} を指定</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>返却インスタンスが元インスタンスと別参照（{@code isNotSameAs}）であること</li>
     *   <li>{@code stampingTime()} が {@code 08:30} に更新されていること</li>
     *   <li>他の全属性（ID、社員番号、氏名、部門、日付等）の値が不変であること</li>
     * </ul>
     */
    @Test
    @DisplayName("正常系: 非 null の打刻時刻を指定した場合、打刻時刻のみ更新された新インスタンスを返却すること")
    void shouldReturnNewInstanceWithUpdatedStampingTime() {
      // [Given]
      LocalTime newStamp = LocalTime.of(8, 30);

      // [When]
      DailyWorkRecord updated = baseRecord.withStampingTime(newStamp);

      // [Then]
      assertThat(updated).isNotSameAs(baseRecord);
      assertThat(updated.stampingTime()).isEqualTo(newStamp);

      // 他の全属性が維持されていることを検証
      assertThat(updated.userId()).isEqualTo(baseRecord.userId());
      assertThat(updated.employeeNumber()).isEqualTo(baseRecord.employeeNumber());
      assertThat(updated.fullName()).isEqualTo(baseRecord.fullName());
      assertThat(updated.departmentName()).isEqualTo(baseRecord.departmentName());
      assertThat(updated.date()).isEqualTo(baseRecord.date());
      assertThat(updated.segmentTitle()).isEqualTo(baseRecord.segmentTitle());
      assertThat(updated.applicationStatus()).isEqualTo(baseRecord.applicationStatus());
      assertThat(updated.actualStartTime()).isEqualTo(baseRecord.actualStartTime());
      assertThat(updated.nextDayStart()).isEqualTo(baseRecord.nextDayStart());
    }

    /**
     * 【テスト観点 / 責務】 境界値・C1 分岐検証。{@code withStampingTime} に {@code null} を指定した場合、
     * インスタンス生成を行わず、自身の参照（{@code this}）をそのまま返却することを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>引数として {@code null} を渡す</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>返却インスタンスが元インスタンスと同一参照（{@code isSameAs}）であること</li>
     * </ul>
     */
    @Test
    @DisplayName("境界値: null を指定した場合、新しいインスタンスを作らず自身（this）をそのまま返却すること")
    void shouldReturnSameInstanceWhenStampingTimeIsNull() {
      // [When]
      DailyWorkRecord actual = baseRecord.withStampingTime(null);

      // [Then]
      assertThat(actual).isSameAs(baseRecord);
    }
  }

  @Nested
  @DisplayName("withDepartmentName 部門名更新シナリオ")
  class WithDepartmentNameScenario {

    /**
     * 【テスト観点 / 責務】 正常系検証。非 null の部門名を指定した際、元のオブジェクトを改変せず、
     * 指定した部門名のみが更新された新しいインスタンスが返却され、他属性が維持されることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>更新用の部門名として {@code "営業本部 ＞ 第一営業部"} を指定</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>返却インスタンスが元インスタンスと別参照（{@code isNotSameAs}）であること</li>
     *   <li>{@code departmentName()} が新しい部門名に更新されていること</li>
     *   <li>他の全属性が維持されていること</li>
     * </ul>
     */
    @Test
    @DisplayName("正常系: 非 null の部門名を指定した場合、部門名のみ更新された新インスタンスを返却すること")
    void shouldReturnNewInstanceWithUpdatedDepartmentName() {
      // [Given]
      String newDept = "営業本部 ＞ 第一営業部";

      // [When]
      DailyWorkRecord updated = baseRecord.withDepartmentName(newDept);

      // [Then]
      assertThat(updated).isNotSameAs(baseRecord);
      assertThat(updated.departmentName()).isEqualTo(newDept);

      // 他の全属性が維持されていることを検証
      assertThat(updated.userId()).isEqualTo(baseRecord.userId());
      assertThat(updated.employeeNumber()).isEqualTo(baseRecord.employeeNumber());
      assertThat(updated.fullName()).isEqualTo(baseRecord.fullName());
      assertThat(updated.date()).isEqualTo(baseRecord.date());
      assertThat(updated.segmentTitle()).isEqualTo(baseRecord.segmentTitle());
      assertThat(updated.applicationStatus()).isEqualTo(baseRecord.applicationStatus());
      assertThat(updated.actualStartTime()).isEqualTo(baseRecord.actualStartTime());
      assertThat(updated.nextDayStart()).isEqualTo(baseRecord.nextDayStart());
      assertThat(updated.stampingTime()).isEqualTo(baseRecord.stampingTime());
    }

    /**
     * 【テスト観点 / 責務】 境界値・C1 分岐検証。{@code withDepartmentName} に {@code null} を指定した場合、
     * インスタンス生成を行わず、自身の参照（{@code this}）をそのまま返却することを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>引数として {@code null} を渡す</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>返却インスタンスが元インスタンスと同一参照（{@code isSameAs}）であること</li>
     * </ul>
     */
    @Test
    @DisplayName("境界値: null を指定した場合、新しいインスタンスを作らず自身（this）をそのまま返却すること")
    void shouldReturnSameInstanceWhenDepartmentNameIsNull() {
      // [When]
      DailyWorkRecord actual = baseRecord.withDepartmentName(null);

      // [Then]
      assertThat(actual).isSameAs(baseRecord);
    }
  }
}
