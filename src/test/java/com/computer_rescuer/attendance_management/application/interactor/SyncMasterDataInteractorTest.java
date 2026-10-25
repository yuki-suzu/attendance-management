package com.computer_rescuer.attendance_management.application.interactor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.computer_rescuer.attendance_management.adapter.out.exception.ExternalIntegrationException;
import com.computer_rescuer.attendance_management.application.port.out.FetchMasterDataPort;
import com.computer_rescuer.attendance_management.application.port.out.MasterDataRepositoryPort;
import com.computer_rescuer.attendance_management.domain.model.Department;
import com.computer_rescuer.attendance_management.domain.model.Segment;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link SyncMasterDataInteractor} の単体テストクラス。
 * <p>
 * 各種マスタデータ（部門マスタ・勤務区分マスタ）の同期ユースケースとして、以下の責務と境界値を網羅的に検証します:
 * <ul>
 *   <li>外部システム（HRMOS）からのマスタデータ一括取得および永続化リポジトリへの委譲</li>
 *   <li>取得データが空（0件）の場合における誤削除抑止のためのスキップ分岐（C1 分岐網羅 100%）</li>
 *   <li>外部 API 通信障害および DB 更新エラー発生時における例外伝播とトランザクション整合性</li>
 * </ul>
 * 本テストは Spring コンテキストを起動せず、純粋な Mockito 単体テストとして実行されます。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SyncMasterDataInteractor 単体テスト")
class SyncMasterDataInteractorTest {

  @Mock
  private FetchMasterDataPort fetchPort;

  @Mock
  private MasterDataRepositoryPort repositoryPort;

  @InjectMocks
  private SyncMasterDataInteractor interactor;

  /**
   * テスト用の部門ドメインモデルを生成するヘルパーメソッド。
   *
   * @param id       部門ID
   * @param name     部門名
   * @param sequence 並び順
   * @return 構築された {@link Department}
   */
  private Department createDepartment(Integer id, String name, Integer sequence) {
    return new Department(id, name, sequence);
  }

  /**
   * テスト用の勤務区分ドメインモデルを生成するヘルパーメソッド。
   *
   * @param id    勤務区分ID
   * @param title 勤務区分名
   * @return 構築された {@link Segment}
   */
  private Segment createSegment(Integer id, String title) {
    return new Segment(
        id,
        title,
        title,
        1,
        LocalTime.of(9, 0),
        LocalTime.of(18, 0)
    );
  }

  @Nested
  @DisplayName("正常系シナリオ")
  class SuccessScenario {

    /**
     * 【テスト観点 / 責務】 正常系全工程の検証。外部システムから部門情報および勤務区分情報が両方とも取得できた場合、
     * それぞれのリポジトリ同期メソッドが正しい引数で順次呼び出されることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>{@code fetchPort.fetchAllDepartments()} が 2 件の部門リストを返却すること</li>
     *   <li>{@code fetchPort.fetchAllSegments()} が 2 件の勤務区分リストを返却すること</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>{@code repositoryPort.syncDepartments} が取得した部門リストを引数として実行されること</li>
     *   <li>{@code repositoryPort.syncSegments} が取得した勤務区分リストを引数として実行されること</li>
     *   <li>渡されたリストの先頭要素が想定通りであること（.getFirst() による検証）</li>
     * </ul>
     */
    @Test
    @DisplayName("正常系: 部門および勤務区分の双方が取得できた場合、両マスタの同期処理が順次実行されること")
    void shouldSyncBothDepartmentsAndSegmentsSuccessfully() {
      // [Given]
      Department dept1 = createDepartment(1, "総務部", 1);
      Department dept2 = createDepartment(2, "開発部", 2);
      List<Department> expectedDepartments = List.of(dept1, dept2);

      Segment seg1 = createSegment(1, "通常出勤");
      Segment seg2 = createSegment(2, "時差出勤");
      List<Segment> expectedSegments = List.of(seg1, seg2);

      when(fetchPort.fetchAllDepartments()).thenReturn(expectedDepartments);
      when(fetchPort.fetchAllSegments()).thenReturn(expectedSegments);

      // [When]
      interactor.syncMasterData();

      // [Then]
      ArgumentCaptor<List<Department>> deptCaptor = ArgumentCaptor.forClass(List.class);
      verify(repositoryPort).syncDepartments(deptCaptor.capture());
      List<Department> actualDepartments = deptCaptor.getValue();
      assertThat(actualDepartments).hasSize(2);
      assertThat(actualDepartments.getFirst().name()).isEqualTo("総務部");

      ArgumentCaptor<List<Segment>> segCaptor = ArgumentCaptor.forClass(List.class);
      verify(repositoryPort).syncSegments(segCaptor.capture());
      List<Segment> actualSegments = segCaptor.getValue();
      assertThat(actualSegments).hasSize(2);
      assertThat(actualSegments.getFirst().title()).isEqualTo("通常出勤");
    }
  }

  @Nested
  @DisplayName("境界値および条件分岐シナリオ（C1 網羅）")
  class BoundaryAndBranchScenario {

    /**
     * 【テスト観点 / 責務】 部門マスタが0件の場合のスキップ分岐検証（C1 分岐網羅）。 外部 API
     * から取得した部門情報が空の場合、部門テーブルの洗い替え（全削除・再登録）を安全にスキップし、 勤務区分マスタの同期のみが実行されることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>{@code fetchPort.fetchAllDepartments()} が空リスト（0件）を返却すること</li>
     *   <li>{@code fetchPort.fetchAllSegments()} が 1 件の勤務区分リストを返却すること</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>{@code repositoryPort.syncDepartments} は一度も呼び出されないこと（never）</li>
     *   <li>{@code repositoryPort.syncSegments} は正常に呼び出されること</li>
     * </ul>
     */
    @Test
    @DisplayName("C1分岐: 取得した部門情報が0件の場合、部門同期をスキップし勤務区分同期のみ実行すること")
    void shouldSkipDepartmentSyncWhenDepartmentListIsEmpty() {
      // [Given]
      Segment seg = createSegment(1, "通常出勤");
      List<Segment> expectedSegments = List.of(seg);

      when(fetchPort.fetchAllDepartments()).thenReturn(Collections.emptyList());
      when(fetchPort.fetchAllSegments()).thenReturn(expectedSegments);

      // [When]
      interactor.syncMasterData();

      // [Then]
      verify(repositoryPort, never()).syncDepartments(anyList());
      verify(repositoryPort).syncSegments(expectedSegments);
    }

    /**
     * 【テスト観点 / 責務】 勤務区分マスタが0件の場合のスキップ分岐検証（C1 分岐網羅）。 外部 API から取得した勤務区分情報が空の場合、部門マスタの同期のみを実行し、
     * 勤務区分テーブルの洗い替えを安全にスキップすることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>{@code fetchPort.fetchAllDepartments()} が 1 件の部門リストを返却すること</li>
     *   <li>{@code fetchPort.fetchAllSegments()} が空リスト（0件）を返却すること</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>{@code repositoryPort.syncDepartments} は正常に呼び出されること</li>
     *   <li>{@code repositoryPort.syncSegments} は一度も呼び出されないこと（never）</li>
     * </ul>
     */
    @Test
    @DisplayName("C1分岐: 取得した勤務区分情報が0件の場合、勤務区分同期をスキップし部門同期のみ実行すること")
    void shouldSkipSegmentSyncWhenSegmentListIsEmpty() {
      // [Given]
      Department dept = createDepartment(1, "人事部", 1);
      List<Department> expectedDepartments = List.of(dept);

      when(fetchPort.fetchAllDepartments()).thenReturn(expectedDepartments);
      when(fetchPort.fetchAllSegments()).thenReturn(Collections.emptyList());

      // [When]
      interactor.syncMasterData();

      // [Then]
      verify(repositoryPort).syncDepartments(expectedDepartments);
      verify(repositoryPort, never()).syncSegments(anyList());
    }

    /**
     * 【テスト観点 / 責務】 両マスタが0件の場合の境界値検証（C1 分岐網羅）。 外部 API からのデータが両方とも空の場合、一切の永続化更新を行わずに
     * 例外を発生させることなく安全に処理を完了することを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>{@code fetchPort.fetchAllDepartments()} が空リストを返却すること</li>
     *   <li>{@code fetchPort.fetchAllSegments()} が空リストを返却すること</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>リポジトリの更新メソッド（{@code syncDepartments}, {@code syncSegments}）は一切呼び出されないこと</li>
     * </ul>
     */
    @Test
    @DisplayName("境界値: 部門・勤務区分の両方が0件の場合、いずれの同期処理も実行せず安全に終了すること")
    void shouldSkipAllSyncWhenBothListsAreEmpty() {
      // [Given]
      when(fetchPort.fetchAllDepartments()).thenReturn(Collections.emptyList());
      when(fetchPort.fetchAllSegments()).thenReturn(Collections.emptyList());

      // [When]
      interactor.syncMasterData();

      // [Then]
      verify(repositoryPort, never()).syncDepartments(anyList());
      verify(repositoryPort, never()).syncSegments(anyList());
    }
  }

  @Nested
  @DisplayName("異常系およびトランザクション境界シナリオ")
  class ExceptionScenario {

    /**
     * 【テスト観点 / 責務】 部門データ取得時の外部通信例外発生シナリオ。 外部 API 呼び出しで {@link ExternalIntegrationException} が発生した際、
     * 例外が上位へそのまま伝播し、後続の勤務区分取得や同期処理が一切行われないことを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>{@code fetchPort.fetchAllDepartments()} が {@link ExternalIntegrationException} をスローすること</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>{@code assertThatThrownBy} により同例外が検知されること</li>
     *   <li>勤務区分の取得およびリポジトリ更新処理は一切行われないこと</li>
     * </ul>
     */
    @Test
    @DisplayName("異常系: 部門マスタ取得時に外部連携例外が発生した場合、例外がそのまま伝播し処理を中断すること")
    void shouldPropagateExceptionWhenFetchingDepartmentsFails() {
      // [Given]
      when(fetchPort.fetchAllDepartments())
          .thenThrow(new ExternalIntegrationException("HRMOS API 認証エラー"));

      // [When & Then]
      assertThatThrownBy(() -> interactor.syncMasterData())
          .isInstanceOf(ExternalIntegrationException.class)
          .hasMessage("HRMOS API 認証エラー");

      verify(fetchPort, never()).fetchAllSegments();
      verifyNoInteractions(repositoryPort);
    }

    /**
     * 【テスト観点 / 責務】 部門マスタ永続化時の DB 障害シナリオ（トランザクションロールバック整合性）。 部門データの DB
     * 保存処理で例外が発生した場合、後続の勤務区分取得・同期が中断され、 例外が上位へスローされることを確認します（{@code @Transactional}
     * によりロールバック対象となる挙動）。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>部門データは正常に取得されること</li>
     *   <li>{@code repositoryPort.syncDepartments} が {@link RuntimeException} をスローすること</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>{@code assertThatThrownBy} により同例外が検知されること</li>
     *   <li>後続の {@code fetchPort.fetchAllSegments()} および勤務区分同期は実行されないこと</li>
     * </ul>
     */
    @Test
    @DisplayName("異常系: 部門マスタ永続化時に例外が発生した場合、勤務区分の処理を行わずに例外をスローすること")
    void shouldPropagateExceptionAndAbortWhenDepartmentPersistenceFails() {
      // [Given]
      Department dept = createDepartment(1, "総務部", 1);
      List<Department> departments = List.of(dept);
      when(fetchPort.fetchAllDepartments()).thenReturn(departments);

      doThrow(new RuntimeException("DB 接続タイムアウト"))
          .when(repositoryPort).syncDepartments(departments);

      // [When & Then]
      assertThatThrownBy(() -> interactor.syncMasterData())
          .isInstanceOf(RuntimeException.class)
          .hasMessage("DB 接続タイムアウト");

      verify(repositoryPort).syncDepartments(departments);
      verify(fetchPort, never()).fetchAllSegments();
      verify(repositoryPort, never()).syncSegments(anyList());
    }

    /**
     * 【テスト観点 / 責務】 勤務区分マスタ永続化時の DB 障害シナリオ（トランザクションロールバック整合性）。 部門同期完了後に勤務区分の DB 保存処理で例外が発生した場合、
     * 例外が上位へスローされ、トランザクション全体がロールバック対象となることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>部門データおよび勤務区分データは正常に取得されること</li>
     *   <li>部門同期は正常に完了すること</li>
     *   <li>{@code repositoryPort.syncSegments} が {@link RuntimeException} をスローすること</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>{@code assertThatThrownBy} により同例外が検知されること</li>
     *   <li>部門同期処理および勤務区分同期処理の両方が呼び出された状態であること</li>
     * </ul>
     */
    @Test
    @DisplayName("異常系: 勤務区分マスタ永続化時に例外が発生した場合、例外が上位へスローされること")
    void shouldPropagateExceptionWhenSegmentPersistenceFails() {
      // [Given]
      Department dept = createDepartment(1, "総務部", 1);
      List<Department> departments = List.of(dept);
      when(fetchPort.fetchAllDepartments()).thenReturn(departments);

      Segment seg = createSegment(1, "通常出勤");
      List<Segment> segments = List.of(seg);
      when(fetchPort.fetchAllSegments()).thenReturn(segments);

      doThrow(new RuntimeException("Unique Constraint Violation"))
          .when(repositoryPort).syncSegments(segments);

      // [When & Then]
      assertThatThrownBy(() -> interactor.syncMasterData())
          .isInstanceOf(RuntimeException.class)
          .hasMessage("Unique Constraint Violation");

      verify(repositoryPort).syncDepartments(departments);
      verify(repositoryPort).syncSegments(segments);
    }
  }
}
