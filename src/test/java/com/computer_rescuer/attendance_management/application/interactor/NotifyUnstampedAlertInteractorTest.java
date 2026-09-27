package com.computer_rescuer.attendance_management.application.interactor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.computer_rescuer.attendance_management.adapter.out.hrmos.mapper.HrmosStampLogMapper;
import com.computer_rescuer.attendance_management.adapter.out.kafka.dto.UnstampedAlertEvent;
import com.computer_rescuer.attendance_management.adapter.out.kafka.dto.UnstampedDirectReminderEvent;
import com.computer_rescuer.attendance_management.adapter.out.kafka.dto.UnstampedDirectReminderEvent.DirectReminderEmployee;
import com.computer_rescuer.attendance_management.adapter.out.kafka.support.UnstampedAlertEventBuilder;
import com.computer_rescuer.attendance_management.application.port.out.CheckedEmployeeRepositoryPort;
import com.computer_rescuer.attendance_management.application.port.out.FetchDailyWorkRecordPort;
import com.computer_rescuer.attendance_management.application.port.out.FetchEmployeeByIdPort;
import com.computer_rescuer.attendance_management.application.port.out.FetchEmployeeDepartmentPort;
import com.computer_rescuer.attendance_management.application.port.out.FetchSegmentPort;
import com.computer_rescuer.attendance_management.application.port.out.FetchStampLogPort;
import com.computer_rescuer.attendance_management.application.port.out.NotifyUnstampedAlertPort;
import com.computer_rescuer.attendance_management.domain.model.DailyAttendance;
import com.computer_rescuer.attendance_management.domain.model.DailyAttendance.Status;
import com.computer_rescuer.attendance_management.domain.model.DailyWorkRecord;
import com.computer_rescuer.attendance_management.domain.model.Employee;
import com.computer_rescuer.attendance_management.domain.model.Segment;
import com.computer_rescuer.attendance_management.domain.service.DailyAttendanceFactory;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link NotifyUnstampedAlertInteractor} の単体テストクラス。
 * <p>
 * ヘキサゴナルアーキテクチャのユースケース実装として、各ポートとの連携、 未打刻者の検知フロー、およびイベント発行制御の命令網羅（C0 100%）を検証します。 Spring
 * コンテキストを起動せず、純粋な Mockito 単体テストとして高速に実行します。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotifyUnstampedAlertInteractor 単体テスト")
class NotifyUnstampedAlertInteractorTest {

  @Mock
  private FetchDailyWorkRecordPort fetchDailyWorkRecordPort;

  @Mock
  private FetchStampLogPort fetchStampLogPort;

  @Mock
  private FetchSegmentPort fetchSegmentPort;

  @Mock
  private FetchEmployeeDepartmentPort fetchEmployeeDepartmentPort;

  @Mock
  private FetchEmployeeByIdPort fetchEmployeeByIdPort;

  @Mock
  private CheckedEmployeeRepositoryPort checkedEmployeeRepositoryPort;

  @Mock
  private NotifyUnstampedAlertPort notifyUnstampedAlertPort;

  @Mock
  private UnstampedAlertEventBuilder eventBuilder;

  @Mock
  private HrmosStampLogMapper stampMapper;

  @Mock
  private DailyAttendanceFactory dailyAttendanceFactory;

  private Clock fixedClock;

  private NotifyUnstampedAlertInteractor interactor;

  private final LocalDate targetDate = LocalDate.of(2026, 4, 1);
  private final ZoneId zoneId = ZoneId.of("Asia/Tokyo");
  private final Instant fixedInstant = Instant.parse("2026-04-01T10:00:00Z");

  /**
   * テストの前準備として、固定クロックの初期化およびテスト対象インスタンスの生成を行います。
   */
  @BeforeEach
  void setUp() {
    fixedClock = Clock.fixed(fixedInstant, zoneId);
    interactor = new NotifyUnstampedAlertInteractor(
        fetchDailyWorkRecordPort,
        fetchStampLogPort,
        fetchSegmentPort,
        fetchEmployeeDepartmentPort,
        fetchEmployeeByIdPort,
        checkedEmployeeRepositoryPort,
        notifyUnstampedAlertPort,
        eventBuilder,
        stampMapper,
        dailyAttendanceFactory,
        fixedClock
    );
  }

  @Nested
  @DisplayName("未打刻者が検知されないシナリオ")
  class NoUnstampedAttendanceScenario {

    /**
     * 未打刻者が0件の場合に早期リターンし、後続の通知ポートやDB更新ポートが呼び出されないことを検証します。
     */
    @Test
    @DisplayName("未打刻者が0件の場合、通知イベント送信や履歴登録を行わずに処理を終了すること")
    void shouldEarlyReturnWhenNoUnstampedAttendanceDetected() {
      // [Given]
      given(fetchDailyWorkRecordPort.fetchByDate(targetDate)).willReturn(Collections.emptyList());
      given(fetchStampLogPort.fetchDailyLogs(targetDate)).willReturn(Collections.emptyList());
      given(fetchSegmentPort.fetchAll()).willReturn(Collections.emptyList());
      given(fetchEmployeeDepartmentPort.fetchDepartmentMapByUserIds(Collections.emptyList()))
          .willReturn(Collections.emptyMap());
      given(stampMapper.toClockInMap(Collections.emptyList()))
          .willReturn(Collections.emptyMap());

      // [When]
      interactor.execute(targetDate);

      // [Then]
      then(fetchEmployeeByIdPort).shouldHaveNoInteractions();
      then(checkedEmployeeRepositoryPort).shouldHaveNoInteractions();
      then(notifyUnstampedAlertPort).shouldHaveNoInteractions();
      then(eventBuilder).shouldHaveNoInteractions();
    }
  }

  @Nested
  @DisplayName("未打刻者が検知されたシナリオ")
  class UnstampedAttendanceDetectedScenario {

    /**
     * 未打刻者が検知され、かつ本人向けDMの送信対象が存在する場合の全パス網羅テスト。
     * <p>
     * C0 100% を達成するため、以下の分岐・変換ロジックを同時に通過させます:
     * <ul>
     *   <li>セグメント重複時のマージ関数 {@code (existing, replacement) -> log.error(...); return existing;} の実行</li>
     *   <li>社員番号 "0000000000" による除外フィルタの true / false 両ルート</li>
     *   <li>出勤ステータス判定による未打刻者フィルタ（LATE_OR_FORGOT）の true / false 両ルート</li>
     *   <li>本人向けDMイベントが空でない場合の送信処理呼び出し</li>
     * </ul>
     * </p>
     */
    @Test
    @DisplayName("未打刻者を検知し、本人DM対象が存在する場合、管理者アラートと本人DMを発行してチェック履歴を保存すること")
    void shouldSendBothAlertAndDirectReminderWhenEligible() {
      // [Given]
      // 1. 勤務区分マスタ（同一名称・異なるIDを持たせてソートと重複マージラムダを通過させる）
      Segment seg1 = mock(Segment.class);
      Segment seg2 = mock(Segment.class);
      given(seg1.id()).willReturn(1);
      given(seg1.title()).willReturn("通常勤務");
      given(seg2.id()).willReturn(2);
      given(seg2.title()).willReturn("通常勤務"); // 重複マージ関数を発火

      given(fetchSegmentPort.fetchAll()).willReturn(List.of(seg2, seg1)); // 逆順で渡しソートも検証

      // 2. 勤務実績データの準備
      // Record 1: システム除外対象（社員番号 "0000000000"） -> filterで除外
      DailyWorkRecord recordSystem = mock(DailyWorkRecord.class);
      given(recordSystem.userId()).willReturn(999);
      given(recordSystem.employeeNumber()).willReturn("0000000000");

      // Record 2: 未打刻対象者
      DailyWorkRecord recordLate = mock(DailyWorkRecord.class);
      given(recordLate.userId()).willReturn(101);
      given(recordLate.employeeNumber()).willReturn("EMP001");

      // Record 3: 未打刻ではない対象者（ATTENDED） -> filterで除外
      DailyWorkRecord recordAttended = mock(DailyWorkRecord.class);
      given(recordAttended.userId()).willReturn(102);
      given(recordAttended.employeeNumber()).willReturn("EMP002");

      List<DailyWorkRecord> records = List.of(recordSystem, recordLate, recordAttended);
      given(fetchDailyWorkRecordPort.fetchByDate(targetDate)).willReturn(records);
      given(fetchStampLogPort.fetchDailyLogs(targetDate)).willReturn(Collections.emptyList());

      Map<Integer, String> departmentMap = Map.of(101, "開発部", 102, "営業部");
      given(fetchEmployeeDepartmentPort.fetchDepartmentMapByUserIds(List.of(999, 101, 102)))
          .willReturn(departmentMap);

      Map<Integer, LocalTime> clockInMap = Map.of(102, LocalTime.of(8, 55));
      given(stampMapper.toClockInMap(Collections.emptyList())).willReturn(clockInMap);

      // 3. Factory による生成オブジェクトのモック
      ZonedDateTime expectedNow = ZonedDateTime.now(fixedClock);

      DailyAttendance attendanceLate = mock(DailyAttendance.class);
      given(attendanceLate.userId()).willReturn(101);
      given(attendanceLate.status()).willReturn(Status.LATE_OR_FORGOT);

      DailyAttendance attendanceAttended = mock(DailyAttendance.class);
      given(attendanceAttended.status()).willReturn(Status.ATTENDED);

      given(dailyAttendanceFactory.create(
          eq(recordLate), any(), eq(departmentMap), eq(clockInMap), eq(targetDate),
          eq(expectedNow)))
          .willReturn(attendanceLate);
      given(dailyAttendanceFactory.create(
          eq(recordAttended), any(), eq(departmentMap), eq(clockInMap), eq(targetDate),
          eq(expectedNow)))
          .willReturn(attendanceAttended);

      // 4. 後続通知ポート・イベントの準備
      Employee employee = mock(Employee.class);
      Map<Integer, Employee> employeeMap = Map.of(101, employee);
      given(fetchEmployeeByIdPort.fetchEmployeeMapByUserIds(List.of(101)))
          .willReturn(employeeMap);

      Set<Integer> checkedTodayIds = Set.of(101);
      given(checkedEmployeeRepositoryPort.findCheckedEmployeeIds(targetDate, List.of(101)))
          .willReturn(checkedTodayIds);

      Map<Integer, Integer> monthlyCounts = Map.of(101, 2);
      given(checkedEmployeeRepositoryPort.countMonthlyChecked(targetDate, List.of(101)))
          .willReturn(monthlyCounts);

      UnstampedAlertEvent managerAlertEvent = mock(UnstampedAlertEvent.class);
      given(eventBuilder.buildManagerAlertEvent(
          eq(targetDate), eq(expectedNow.toLocalDateTime()), eq(List.of(attendanceLate)),
          eq(employeeMap), eq(checkedTodayIds), eq(monthlyCounts)))
          .willReturn(managerAlertEvent);

      UnstampedDirectReminderEvent directReminderEvent = mock(UnstampedDirectReminderEvent.class);
      given(directReminderEvent.employees()).willReturn(
          List.of(mock(DirectReminderEmployee.class))); // 宛先あり
      given(eventBuilder.buildDirectReminderEvent(
          List.of(attendanceLate), employeeMap, checkedTodayIds))
          .willReturn(directReminderEvent);

      // [When]
      interactor.execute(targetDate);

      // [Then]
      then(notifyUnstampedAlertPort).should().sendManagerAlert(managerAlertEvent);
      then(notifyUnstampedAlertPort).should().sendDirectReminder(directReminderEvent);
      then(checkedEmployeeRepositoryPort).should().saveAll(
          targetDate,
          List.of(101),
          CheckedEmployeeRepositoryPort.REASON_CODE_UNSTAMPED
      );
    }

    /**
     * 未打刻者は検知されたが、本人DMの宛先が空（既に全員送信済み等）の場合に、 本人DM送信のみ安全にスキップされ、管理者アラート送信と履歴保存は正常に行われることを検証します。
     */
    @Test
    @DisplayName("本人DM対象者が空の場合、本人向けDM送信をスキップし管理者アラートのみ送信すること")
    void shouldSkipDirectReminderWhenDirectEventEmployeesIsEmpty() {
      // [Given]
      Segment segment = mock(Segment.class);
      // 要素数1のリストではソート時に Comparator(Segment::id) が評価されないため、title のみスタブを設定する
      given(segment.title()).willReturn("裁量労働");
      given(fetchSegmentPort.fetchAll()).willReturn(List.of(segment));

      DailyWorkRecord record = mock(DailyWorkRecord.class);
      given(record.userId()).willReturn(201);
      given(record.employeeNumber()).willReturn("EMP201");

      given(fetchDailyWorkRecordPort.fetchByDate(targetDate)).willReturn(List.of(record));
      given(fetchStampLogPort.fetchDailyLogs(targetDate)).willReturn(Collections.emptyList());
      given(fetchEmployeeDepartmentPort.fetchDepartmentMapByUserIds(List.of(201))).willReturn(
          Collections.emptyMap());
      given(stampMapper.toClockInMap(Collections.emptyList())).willReturn(Collections.emptyMap());

      DailyAttendance attendance = mock(DailyAttendance.class);
      given(attendance.userId()).willReturn(201);
      given(attendance.status()).willReturn(Status.LATE_OR_FORGOT);

      ZonedDateTime expectedNow = ZonedDateTime.now(fixedClock);
      given(dailyAttendanceFactory.create(any(), any(), any(), any(), eq(targetDate),
          eq(expectedNow)))
          .willReturn(attendance);

      Employee employee = mock(Employee.class);
      Map<Integer, Employee> employeeMap = Map.of(201, employee);
      given(fetchEmployeeByIdPort.fetchEmployeeMapByUserIds(List.of(201))).willReturn(employeeMap);

      Set<Integer> checkedTodayIds = Set.of(201);
      given(checkedEmployeeRepositoryPort.findCheckedEmployeeIds(targetDate, List.of(201)))
          .willReturn(checkedTodayIds);
      given(checkedEmployeeRepositoryPort.countMonthlyChecked(targetDate, List.of(201)))
          .willReturn(Map.of(201, 1));

      UnstampedAlertEvent managerAlertEvent = mock(UnstampedAlertEvent.class);
      given(eventBuilder.buildManagerAlertEvent(any(), any(), any(), any(), any(), any()))
          .willReturn(managerAlertEvent);

      // 本人DM対象が空リストのイベント
      UnstampedDirectReminderEvent directReminderEvent = mock(UnstampedDirectReminderEvent.class);
      given(directReminderEvent.employees()).willReturn(Collections.emptyList());
      given(eventBuilder.buildDirectReminderEvent(any(), any(), any()))
          .willReturn(directReminderEvent);

      // [When]
      interactor.execute(targetDate);

      // [Then]
      then(notifyUnstampedAlertPort).should().sendManagerAlert(managerAlertEvent);
      then(notifyUnstampedAlertPort).should(never()).sendDirectReminder(any());
      then(checkedEmployeeRepositoryPort).should().saveAll(
          targetDate,
          List.of(201),
          CheckedEmployeeRepositoryPort.REASON_CODE_UNSTAMPED
      );
    }
  }
}
