package com.computer_rescuer.attendance_management.adapter.out.kafka.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.computer_rescuer.attendance_management.adapter.out.kafka.dto.UnstampedAlertEvent;
import com.computer_rescuer.attendance_management.adapter.out.kafka.dto.UnstampedDirectReminderEvent;
import com.computer_rescuer.attendance_management.domain.model.DailyAttendance;
import com.computer_rescuer.attendance_management.domain.model.DailyAttendance.Status;
import com.computer_rescuer.attendance_management.domain.model.Employee;
import com.computer_rescuer.attendance_management.infrastructure.property.KafkaProperties;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link UnstampedAlertEventBuilder} の単体テストクラス。
 * <p>
 * 未打刻判定結果から管理者向けサマリーアラート（{@link UnstampedAlertEvent}）および
 * 本人向けダイレクトDMリマインド（{@link UnstampedDirectReminderEvent}）を構築するビルダーの責務を検証します。 主な検証観点は以下の通りです:
 * <ul>
 *   <li>管理者アラートにおける、本日チェック状態（当日初回検知 vs 当日再検知）に応じた当月累積未打刻回数の補正ロジック</li>
 *   <li>従業員マスタ（{@link Employee}）未ヒット時における email の null フォールバック処理</li>
 *   <li>{@link KafkaProperties#directReminderEnabled()} のトグル値（true / false）に応じた代理送信アドレス（proxy@example.com）への切り替え</li>
 *   <li>本日すでに通知済みの従業員に対する本人DMイベントの安全な除外フィルタリング</li>
 *   <li>入力リストが空の場合の境界値ハンドリング</li>
 * </ul>
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UnstampedAlertEventBuilder 単体テスト")
class UnstampedAlertEventBuilderTest {

  @Mock
  private KafkaProperties kafkaProperties;

  @InjectMocks
  private UnstampedAlertEventBuilder eventBuilder;

  private final LocalDate targetDate = LocalDate.of(2026, 4, 20);
  private final LocalDateTime detectedAt = LocalDateTime.of(2026, 4, 20, 10, 0, 0);

  /**
   * テスト用の {@link DailyAttendance} インスタンスを生成するヘルパーメソッド。
   *
   * @param userId         従業員内部ID
   * @param employeeNumber 社員番号
   * @param fullName       氏名
   * @param scheduledStart 予定出勤時刻
   * @return 構築された {@link DailyAttendance}
   */
  private DailyAttendance createAttendance(
      Integer userId,
      String employeeNumber,
      String fullName,
      LocalTime scheduledStart
  ) {
    return new DailyAttendance(
        userId,
        employeeNumber,
        fullName,
        "システム開発部",
        "通常勤務",
        Status.LATE_OR_FORGOT,
        scheduledStart,
        null,
        null
    );
  }

  /**
   * テスト用の {@link Employee} インスタンスを生成するヘルパーメソッド。
   *
   * @param id    従業員内部ID
   * @param email メールアドレス
   * @return 構築された {@link Employee}
   */
  private Employee createEmployee(Integer id, String email) {
    return new Employee(
        id,
        "EMP" + id,
        "山田",
        "太郎",
        email,
        1,
        1,
        1
    );
  }

  @Nested
  @DisplayName("管理者向けアラートイベント構築（buildManagerAlertEvent）の検証")
  class BuildManagerAlertEventTest {

    /**
     * 【テスト観点 / 責務】 本日初回検知の従業員に対する管理者アラート構築検証。 当日未チェック（{@code alreadyCheckedTodayIds} に含まれない）の場合、
     * 当月の累積回数が +1 加算されて構築されること、およびマスタのメールアドレスが設定されることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>従業員A: 当月過去カウント2回、本日未チェック</li>
     *   <li>従業員B: 当月過去カウント0回（今月初）、本日未チェック</li>
     *   <li>両名とも従業員マスタに有効なメールアドレスが登録されていること</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>従業員Aの当月未打刻回数が 3 (2 + 1) となること</li>
     *   <li>従業員Bの当月未打刻回数が 1 (0 + 1) となること</li>
     *   <li>ヘッダーの対象日、検知日時、各従業員の属性値が完全に一致すること</li>
     * </ul>
     */
    @Test
    @DisplayName("正常系: 本日初回検知の場合、過去の当月回数に1が加算されてイベントが構築されること")
    void shouldBuildManagerAlertEventForFirstTimeDetection() {
      // [Given]
      DailyAttendance attendanceA = createAttendance(101, "EMP101", "山田 太郎",
          LocalTime.of(9, 0));
      DailyAttendance attendanceB = createAttendance(102, "EMP102", "佐藤 次郎",
          LocalTime.of(9, 30));
      List<DailyAttendance> alerts = List.of(attendanceA, attendanceB);

      Map<Integer, Employee> employeeMap = Map.of(
          101, createEmployee(101, "yamada@example.com"),
          102, createEmployee(102, "sato@example.com")
      );

      Set<Integer> alreadyCheckedTodayIds = Set.of(); // 本日未チェック
      Map<Integer, Integer> monthlyCounts = Map.of(
          101, 2 // 過去2回
          // 102 はキーなし（過去0回）
      );

      // [When]
      UnstampedAlertEvent actual = eventBuilder.buildManagerAlertEvent(
          targetDate,
          detectedAt,
          alerts,
          employeeMap,
          alreadyCheckedTodayIds,
          monthlyCounts
      );

      // [Then]
      assertThat(actual.targetDate()).isEqualTo(targetDate);
      assertThat(actual.detectedAt()).isEqualTo(detectedAt);
      assertThat(actual.employees()).hasSize(2);

      UnstampedAlertEvent.UnstampedEmployee empA = actual.employees().getFirst();
      assertThat(empA.employeeNumber()).isEqualTo("EMP101");
      assertThat(empA.email()).isEqualTo("yamada@example.com");
      assertThat(empA.departmentName()).isEqualTo("システム開発部");
      assertThat(empA.fullName()).isEqualTo("山田 太郎");
      assertThat(empA.scheduledStartAt()).isEqualTo(LocalTime.of(9, 0));
      assertThat(empA.monthlyUnstampedCount()).isEqualTo(3); // 2 + 1

      UnstampedAlertEvent.UnstampedEmployee empB = actual.employees().get(1);
      assertThat(empB.employeeNumber()).isEqualTo("EMP102");
      assertThat(empB.email()).isEqualTo("sato@example.com");
      assertThat(empB.fullName()).isEqualTo("佐藤 次郎");
      assertThat(empB.monthlyUnstampedCount()).isEqualTo(1); // 0 + 1

      verifyNoInteractions(kafkaProperties);
    }

    /**
     * 【テスト観点 / 責務】 本日再検知（チェック済み）の従業員に対する当月カウント補正（C1 分岐）の検証。
     * 当日既にチェック済み（{@code alreadyCheckedTodayIds} に含まれる）の場合、 当日分の多重加算を抑止して
     * {@code Math.max(baseCount, 1)} が適用されることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>従業員A: 本日チェック済み、当月過去カウント 3 回（Math.max(3, 1) = 3）</li>
     *   <li>従業員B: 本日チェック済み、当月過去カウント 0 回（Math.max(0, 1) = 1: 最低保証）</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>従業員Aの回数が 3 のまま維持されること（多重インクリメントの抑止）</li>
     *   <li>従業員Bの回数が 1 となること（チェック済みであるため0ではなく最低1）</li>
     * </ul>
     */
    @Test
    @DisplayName("C1分岐: 本日既にチェック済みの従業員は当月回数が二重加算されずMath.max(baseCount, 1)が適用されること")
    void shouldBuildManagerAlertEventForAlreadyCheckedToday() {
      // [Given]
      DailyAttendance attendanceA = createAttendance(101, "EMP101", "山田 太郎",
          LocalTime.of(9, 0));
      DailyAttendance attendanceB = createAttendance(102, "EMP102", "佐藤 次郎",
          LocalTime.of(9, 30));
      List<DailyAttendance> alerts = List.of(attendanceA, attendanceB);

      Map<Integer, Employee> employeeMap = Map.of(
          101, createEmployee(101, "yamada@example.com"),
          102, createEmployee(102, "sato@example.com")
      );

      Set<Integer> alreadyCheckedTodayIds = Set.of(101, 102); // 両名ともチェック済み
      Map<Integer, Integer> monthlyCounts = Map.of(
          101, 3 // 過去カウント 3
          // 102 はキーなし（0）
      );

      // [When]
      UnstampedAlertEvent actual = eventBuilder.buildManagerAlertEvent(
          targetDate,
          detectedAt,
          alerts,
          employeeMap,
          alreadyCheckedTodayIds,
          monthlyCounts
      );

      // [Then]
      assertThat(actual.employees()).hasSize(2);
      assertThat(actual.employees().getFirst().monthlyUnstampedCount()).isEqualTo(
          3); // Math.max(3, 1) -> 3
      assertThat(actual.employees().get(1).monthlyUnstampedCount()).isEqualTo(
          1);    // Math.max(0, 1) -> 1
    }

    /**
     * 【テスト観点 / 責務】 従業員マスタ不在時の null 安全性（C1 分岐）の検証。 {@code employeeMap} に対象の userId が存在しない場合でも、
     * {@link NullPointerException} を発生させずに email が null として安全にマッピングされることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>未打刻者（userId: 999）が存在するが、{@code employeeMap} は空であること</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>構築された従業員情報の email が null となること</li>
     * </ul>
     */
    @Test
    @DisplayName("C1分岐: 従業員マスタに未登録のuserIdが存在する場合、emailがnullで安全に構築されること")
    void shouldHandleMissingEmployeeInMapForManagerAlert() {
      // [Given]
      DailyAttendance attendance = createAttendance(999, "EMP999", "不明 ユーザー",
          LocalTime.of(9, 0));
      List<DailyAttendance> alerts = List.of(attendance);

      // [When]
      UnstampedAlertEvent actual = eventBuilder.buildManagerAlertEvent(
          targetDate,
          detectedAt,
          alerts,
          Collections.emptyMap(),
          Collections.emptySet(),
          Collections.emptyMap()
      );

      // [Then]
      assertThat(actual.employees()).hasSize(1);
      assertThat(actual.employees().getFirst().email()).isNull();
      assertThat(actual.employees().getFirst().employeeNumber()).isEqualTo("EMP999");
    }

    /**
     * 【テスト観点 / 責務】 未打刻者リストが空の場合の境界値検証。 空の従業員リストを持つ {@link UnstampedAlertEvent} が安全に返却されることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>{@code alerts} が空リストであること</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>戻り値の {@code employees()} が空リストであること</li>
     * </ul>
     */
    @Test
    @DisplayName("境界値: 未打刻勤怠リストが空の場合、空の従業員リストを持つイベントを返却すること")
    void shouldReturnEmptyEventWhenAlertsIsEmptyForManagerAlert() {
      // [When]
      UnstampedAlertEvent actual = eventBuilder.buildManagerAlertEvent(
          targetDate,
          detectedAt,
          Collections.emptyList(),
          Collections.emptyMap(),
          Collections.emptySet(),
          Collections.emptyMap()
      );

      // [Then]
      assertThat(actual.targetDate()).isEqualTo(targetDate);
      assertThat(actual.detectedAt()).isEqualTo(detectedAt);
      assertThat(actual.employees()).isEmpty();
    }
  }

  @Nested
  @DisplayName("本人向けDMリマインドイベント構築（buildDirectReminderEvent）の検証")
  class BuildDirectReminderEventTest {

    /**
     * 【テスト観点 / 責務】 本人DM有効時（{@code directReminderEnabled = true}）の抽出およびフィルタリング検証。
     * 本日未チェック者のみが抽出され、従業員マスタのメールアドレスがそのまま設定されること、 および本日既チェック者が確実に除外されることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>{@code directReminderEnabled} が true であること</li>
     *   <li>未打刻者2名（101: 本日未チェック, 102: 本日既チェック）が存在すること</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>本日チェック済みの 102 が除外され、未チェックの 101 のみが出力されること</li>
     *   <li>101 のメールアドレスとしてマスタの値（"yamada@example.com"）が設定されること</li>
     * </ul>
     */
    @Test
    @DisplayName("正常系: DM有効時、本日未チェック者のみが抽出されマスタのemailが設定されること")
    void shouldBuildDirectReminderWhenEnabledAndNotCheckedToday() {
      // [Given]
      when(kafkaProperties.directReminderEnabled()).thenReturn(true);

      DailyAttendance attendanceA = createAttendance(101, "EMP101", "山田 太郎",
          LocalTime.of(9, 0));
      DailyAttendance attendanceB = createAttendance(102, "EMP102", "佐藤 次郎",
          LocalTime.of(9, 0));
      List<DailyAttendance> alerts = List.of(attendanceA, attendanceB);

      Map<Integer, Employee> employeeMap = Map.of(
          101, createEmployee(101, "yamada@example.com"),
          102, createEmployee(102, "sato@example.com")
      );

      Set<Integer> alreadyCheckedTodayIds = Set.of(102); // 102 は本日チェック済み

      // [When]
      UnstampedDirectReminderEvent actual = eventBuilder.buildDirectReminderEvent(
          alerts,
          employeeMap,
          alreadyCheckedTodayIds
      );

      // [Then]
      assertThat(actual.employees()).hasSize(1);
      UnstampedDirectReminderEvent.DirectReminderEmployee directEmp = actual.employees().getFirst();
      assertThat(directEmp.employeeNumber()).isEqualTo("EMP101");
      assertThat(directEmp.fullName()).isEqualTo("山田 太郎");
      assertThat(directEmp.email()).isEqualTo("yamada@example.com");
    }

    /**
     * 【テスト観点 / 責務】 本人DM無効時（{@code directReminderEnabled = false}）の代理送信モード分岐（C1 分岐）の検証。
     * 設定が無効の場合、notification-service 側で管理者代理送信として扱わせるため、 メールアドレスが強制的に {@code "proxy@example.com"}
     * に置換されることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>{@code directReminderEnabled} が false であること</li>
     *   <li>従業員マスタには実在するメールアドレスが登録されていること</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>マスタの値に関わらず、email が "proxy@example.com" に固定されること</li>
     * </ul>
     */
    @Test
    @DisplayName("C1分岐: DM無効設定の場合、未チェック従業員のメールアドレスが強制的にproxy@example.comになること")
    void shouldSetProxyEmailWhenDirectReminderIsDisabled() {
      // [Given]
      when(kafkaProperties.directReminderEnabled()).thenReturn(false);

      DailyAttendance attendance = createAttendance(101, "EMP101", "山田 太郎", LocalTime.of(9, 0));
      List<DailyAttendance> alerts = List.of(attendance);

      Map<Integer, Employee> employeeMap = Map.of(
          101, createEmployee(101, "real-email@example.com")
      );

      // [When]
      UnstampedDirectReminderEvent actual = eventBuilder.buildDirectReminderEvent(
          alerts,
          employeeMap,
          Collections.emptySet()
      );

      // [Then]
      assertThat(actual.employees()).hasSize(1);
      assertThat(actual.employees().getFirst().email()).isEqualTo("proxy@example.com");
    }

    /**
     * 【テスト観点 / 責務】 DM有効時において従業員マスタが存在しない場合の null 安全性（C1 分岐）の検証。
     * {@code directReminderEnabled = true} の状態で {@code employeeMap} に該当者が存在しない場合、 email が null
     * で安全に設定されることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>{@code directReminderEnabled} が true であること</li>
     *   <li>{@code employeeMap} が空マップであること</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>email が null となること</li>
     * </ul>
     */
    @Test
    @DisplayName("C1分岐: DM有効時に従業員マスタが存在しない場合、emailがnullで構築されること")
    void shouldSetNullEmailWhenEmployeeNotFoundAndEnabled() {
      // [Given]
      when(kafkaProperties.directReminderEnabled()).thenReturn(true);

      DailyAttendance attendance = createAttendance(101, "EMP101", "山田 太郎", LocalTime.of(9, 0));
      List<DailyAttendance> alerts = List.of(attendance);

      // [When]
      UnstampedDirectReminderEvent actual = eventBuilder.buildDirectReminderEvent(
          alerts,
          Collections.emptyMap(),
          Collections.emptySet()
      );

      // [Then]
      assertThat(actual.employees()).hasSize(1);
      assertThat(actual.employees().getFirst().email()).isNull();
    }

    /**
     * 【テスト観点 / 責務】 全未打刻者が本日チェック済みの場合の除外境界値検証。 すべての従業員がフィルタで弾かれ、空リストを持つイベントが返却されることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>{@code directReminderEnabled} が true であること</li>
     *   <li>未打刻者全員の userId が {@code alreadyCheckedTodayIds} に含まれていること</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>戻り値の {@code employees()} が空リストであること</li>
     * </ul>
     */
    @Test
    @DisplayName("境界値: 全員が本日チェック済みの場合、空リストのイベントを返却すること")
    void shouldReturnEmptyEventWhenAllEmployeesAlreadyChecked() {
      // [Given]
      when(kafkaProperties.directReminderEnabled()).thenReturn(true);

      DailyAttendance attendance = createAttendance(101, "EMP101", "山田 太郎", LocalTime.of(9, 0));
      List<DailyAttendance> alerts = List.of(attendance);
      Set<Integer> alreadyCheckedTodayIds = Set.of(101);

      // [When]
      UnstampedDirectReminderEvent actual = eventBuilder.buildDirectReminderEvent(
          alerts,
          Collections.emptyMap(),
          alreadyCheckedTodayIds
      );

      // [Then]
      assertThat(actual.employees()).isEmpty();
    }

    /**
     * 【テスト観点 / 責務】 未打刻者リストが空の場合の境界値検証。 空リストを持つ {@link UnstampedDirectReminderEvent}
     * が返却されることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>{@code alerts} が空リストであること</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>戻り値の {@code employees()} が空リストであること</li>
     * </ul>
     */
    @Test
    @DisplayName("境界値: 未打刻勤怠リストが空の場合、空リストのイベントを返却すること")
    void shouldReturnEmptyEventWhenAlertsIsEmptyForDirectReminder() {
      // [Given]
      when(kafkaProperties.directReminderEnabled()).thenReturn(true);

      // [When]
      UnstampedDirectReminderEvent actual = eventBuilder.buildDirectReminderEvent(
          Collections.emptyList(),
          Collections.emptyMap(),
          Collections.emptySet()
      );

      // [Then]
      assertThat(actual.employees()).isEmpty();
    }
  }
}
