package com.computer_rescuer.attendance_management.application.interactor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.computer_rescuer.attendance_management.adapter.out.exception.ExternalIntegrationException;
import com.computer_rescuer.attendance_management.application.port.in.GetDailyAttendanceUseCase;
import com.computer_rescuer.attendance_management.domain.model.DailyAttendance;
import com.computer_rescuer.attendance_management.domain.model.DailyAttendance.Status;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link ExtractUnstampedUsersInteractor} の単体テストクラス。
 * <p>
 * 本クラスは出勤状況取得ユースケース（{@link GetDailyAttendanceUseCase}）から全従業員の出勤判定結果を取得し、 ドメインモデルによって「遅刻 /
 * 打刻忘れ（{@link Status#LATE_OR_FORGOT}）」と判定された従業員のみを 正確にフィルタリングして抽出する責務を持ちます。
 * <p>
 * 以下のテスト観点を網羅的に検証します:
 * <ul>
 *   <li>各ステータス（出勤済、未出勤、遅刻/打刻忘れ）混在時のフィルタリング（C1 条件分岐 True/False 網羅）</li>
 *   <li>未打刻者が0件の場合の境界値挙動（空リスト返却）</li>
 *   <li>全員が未打刻者の場合の全件抽出</li>
 *   <li>取得元の勤怠リストが空の場合の境界値挙動</li>
 *   <li>日付パラメータ（null 含む）の透過的引き渡し</li>
 *   <li>依存ユースケースで例外が発生した場合の透過的伝播（異常系）</li>
 * </ul>
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ExtractUnstampedUsersInteractor 単体テスト")
class ExtractUnstampedUsersInteractorTest {

  @Mock
  private GetDailyAttendanceUseCase getDailyAttendanceUseCase;

  @InjectMocks
  private ExtractUnstampedUsersInteractor interactor;

  private final LocalDate targetDate = LocalDate.of(2026, 4, 20);

  @Nested
  @DisplayName("未打刻者抽出フィルタリングシナリオ")
  class ExtractFilterScenario {

    /**
     * 【テスト観点 / 責務】 正常系・C1 分岐網羅検証。 様々な出勤ステータス（ATTENDED, NOT_ATTENDED, LATE_OR_FORGOT）が混在しているリストから、
     * ステータスが {@link Status#LATE_OR_FORGOT} である従業員のみが過不足なく抽出され、 その他のステータスの従業員が確実に除外されることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>出勤済（ATTENDED）の従業員が1名</li>
     *   <li>未出勤（NOT_ATTENDED）の従業員が1名</li>
     *   <li>遅刻/打刻忘れ（LATE_OR_FORGOT）の従業員が2名（EMP001, EMP002）</li>
     *   <li>上記4名を含むリストを {@code getDailyAttendanceUseCase.execute(targetDate)} が返却するよう設定</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>返却リストの件数が 2 件であること</li>
     *   <li>抽出された要素がすべて {@link Status#LATE_OR_FORGOT} を持つインスタンスであること</li>
     *   <li>ATTENDED および NOT_ATTENDED のインスタンスが含まれていないこと</li>
     *   <li>{@code getDailyAttendanceUseCase.execute(targetDate)} が指定された日付で1回呼び出されること</li>
     * </ul>
     */
    @Test
    @DisplayName("正常系: 複数ステータス混在時、LATE_OR_FORGOT の従業員のみを抽出して返却すること")
    void shouldExtractOnlyLateOrForgotUsers() {
      // [Given]
      DailyAttendance attendedUser = mock(DailyAttendance.class);
      when(attendedUser.status()).thenReturn(Status.ATTENDED);

      DailyAttendance notAttendedUser = mock(DailyAttendance.class);
      when(notAttendedUser.status()).thenReturn(Status.NOT_ATTENDED);

      DailyAttendance lateUser1 = mock(DailyAttendance.class);
      when(lateUser1.status()).thenReturn(Status.LATE_OR_FORGOT);

      DailyAttendance lateUser2 = mock(DailyAttendance.class);
      when(lateUser2.status()).thenReturn(Status.LATE_OR_FORGOT);

      List<DailyAttendance> allAttendances = List.of(
          attendedUser,
          lateUser1,
          notAttendedUser,
          lateUser2
      );

      when(getDailyAttendanceUseCase.execute(targetDate)).thenReturn(allAttendances);

      // [When]
      List<DailyAttendance> actual = interactor.execute(targetDate);

      // [Then]
      assertThat(actual)
          .isNotNull()
          .hasSize(2)
          .containsExactly(lateUser1, lateUser2)
          .doesNotContain(attendedUser, notAttendedUser);

      assertThat(actual.getFirst().status()).isEqualTo(Status.LATE_OR_FORGOT);
      assertThat(actual.get(1).status()).isEqualTo(Status.LATE_OR_FORGOT);

      verify(getDailyAttendanceUseCase).execute(targetDate);
    }

    /**
     * 【テスト観点 / 責務】 境界値検証（全件未打刻）。 取得された全従業員のステータスが {@link Status#LATE_OR_FORGOT} の場合、
     * 1件も漏れることなく全員が抽出されて返却されることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>未打刻者（LATE_OR_FORGOT）2名を含むリストを用意</li>
     *   <li>{@code getDailyAttendanceUseCase.execute(targetDate)} が上記リストを返却するよう設定</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>返却リストの件数が元のリスト件数（2件）と完全に一致すること</li>
     *   <li>取得元リストの要素が順序を含めてそのまま保持されていること</li>
     * </ul>
     */
    @Test
    @DisplayName("境界値: 全員が LATE_OR_FORGOT の場合、全件がそのまま返却されること")
    void shouldReturnAllUsersWhenAllAreLateOrForgot() {
      // [Given]
      DailyAttendance lateUser1 = mock(DailyAttendance.class);
      when(lateUser1.status()).thenReturn(Status.LATE_OR_FORGOT);

      DailyAttendance lateUser2 = mock(DailyAttendance.class);
      when(lateUser2.status()).thenReturn(Status.LATE_OR_FORGOT);

      List<DailyAttendance> allAttendances = List.of(lateUser1, lateUser2);
      when(getDailyAttendanceUseCase.execute(targetDate)).thenReturn(allAttendances);

      // [When]
      List<DailyAttendance> actual = interactor.execute(targetDate);

      // [Then]
      assertThat(actual)
          .hasSize(2)
          .containsExactly(lateUser1, lateUser2);

      verify(getDailyAttendanceUseCase).execute(targetDate);
    }

    /**
     * 【テスト観点 / 責務】 境界値検証（未打刻者なし）。 従業員は存在するが出勤済または未出勤のみで、未打刻者（{@link Status#LATE_OR_FORGOT}）が
     * 1名も存在しない場合、安全に空のリストが返却されることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>出勤済（ATTENDED）1名、未出勤（NOT_ATTENDED）1名を含むリストを用意</li>
     *   <li>{@code getDailyAttendanceUseCase.execute(targetDate)} が上記リストを返却するよう設定</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>返却リストが null ではなく、空リスト（size = 0）であること</li>
     * </ul>
     */
    @Test
    @DisplayName("境界値: 未打刻者が0件の場合、空のリストを返却すること")
    void shouldReturnEmptyListWhenNoUserIsLateOrForgot() {
      // [Given]
      DailyAttendance attendedUser = mock(DailyAttendance.class);
      when(attendedUser.status()).thenReturn(Status.ATTENDED);

      DailyAttendance notAttendedUser = mock(DailyAttendance.class);
      when(notAttendedUser.status()).thenReturn(Status.NOT_ATTENDED);

      List<DailyAttendance> allAttendances = List.of(attendedUser, notAttendedUser);
      when(getDailyAttendanceUseCase.execute(targetDate)).thenReturn(allAttendances);

      // [When]
      List<DailyAttendance> actual = interactor.execute(targetDate);

      // [Then]
      assertThat(actual)
          .isNotNull()
          .isEmpty();

      verify(getDailyAttendanceUseCase).execute(targetDate);
    }

    /**
     * 【テスト観点 / 責務】 境界値検証（元データ空リスト）。 取得元の出勤状況リストが空（0件）の場合、例外をスローせず安全に空のリストを返却することを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>{@code getDailyAttendanceUseCase.execute(targetDate)} が空リストを返却するよう設定</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>返却リストが null ではなく、空リスト（size = 0）であること</li>
     * </ul>
     */
    @Test
    @DisplayName("境界値: 取得元の出勤状況が空リストの場合、空リストを返却すること")
    void shouldReturnEmptyListWhenAllAttendancesIsEmpty() {
      // [Given]
      when(getDailyAttendanceUseCase.execute(targetDate)).thenReturn(Collections.emptyList());

      // [When]
      List<DailyAttendance> actual = interactor.execute(targetDate);

      // [Then]
      assertThat(actual)
          .isNotNull()
          .isEmpty();

      verify(getDailyAttendanceUseCase).execute(targetDate);
    }

    /**
     * 【テスト観点 / 責務】 境界値検証（対象日 null の透過的受け渡し）。 引数 {@code targetDate} に null
     * が渡された場合でも、ユースケース内で例外を起こさず、 下位の {@link GetDailyAttendanceUseCase} へそのまま null を委譲することを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>{@code targetDate} を null として呼び出す</li>
     *   <li>{@code getDailyAttendanceUseCase.execute(null)} が空リストを返却するよう設定</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>例外なく空リストが返却されること</li>
     *   <li>{@code getDailyAttendanceUseCase.execute(null)} が呼び出されること</li>
     * </ul>
     */
    @Test
    @DisplayName("境界値: targetDate が null の場合でも、下位ユースケースへそのまま委譲されること")
    void shouldDelegateDirectlyWhenTargetDateIsNull() {
      // [Given]
      LocalDate nullDate = null;
      when(getDailyAttendanceUseCase.execute(nullDate)).thenReturn(Collections.emptyList());

      // [When]
      List<DailyAttendance> actual = interactor.execute(nullDate);

      // [Then]
      assertThat(actual).isEmpty();
      verify(getDailyAttendanceUseCase).execute(nullDate);
    }
  }

  @Nested
  @DisplayName("異常系シナリオ")
  class ExceptionScenario {

    /**
     * 【テスト観点 / 責務】 異常系検証（外部連携例外の透過的伝播）。 下位の {@link GetDailyAttendanceUseCase#execute(LocalDate)} で
     * {@link ExternalIntegrationException} 等の例外が発生した場合、Interactor
     * が例外を握り潰すことなく、そのまま上位へスローすることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>{@code getDailyAttendanceUseCase.execute(targetDate)} が {@link ExternalIntegrationException} をスローするよう設定</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>{@code assertThatThrownBy} により同例外がキャッチされること</li>
     *   <li>例外メッセージが保持されていること</li>
     * </ul>
     */
    @Test
    @DisplayName("異常系: 下位ユースケースで例外が発生した場合、例外をそのままスローすること")
    void shouldPropagateExceptionWhenUseCaseThrowsException() {
      // [Given]
      String errorMessage = "HRMOS 勤怠実績取得 API 通信エラー";
      when(getDailyAttendanceUseCase.execute(targetDate))
          .thenThrow(new ExternalIntegrationException(errorMessage));

      // [When & Then]
      assertThatThrownBy(() -> interactor.execute(targetDate))
          .isInstanceOf(ExternalIntegrationException.class)
          .hasMessage(errorMessage);

      verify(getDailyAttendanceUseCase).execute(targetDate);
    }
  }
}
