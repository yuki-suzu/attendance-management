package com.computer_rescuer.attendance_management.adapter.out.persistence;

import static com.computer_rescuer.attendance_management.generated.jooq.Tables.M_SEGMENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.computer_rescuer.attendance_management.adapter.out.persistence.mapper.SegmentJooqMapper;
import com.computer_rescuer.attendance_management.domain.model.Segment;
import com.computer_rescuer.attendance_management.generated.jooq.tables.records.MSegmentRecord;
import java.util.List;
import java.util.Objects;
import org.jooq.DSLContext;
import org.jooq.RecordMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link SegmentPersistenceAdapter} の単体テストクラス。
 * <p>
 * 勤務区分マスタ取得出力ポート（{@link
 * com.computer_rescuer.attendance_management.application.port.out.FetchSegmentPort}）の実装として、 jOOQ
 * クエリ（M_SEGMENT の ID 昇順全件検索）およびマッパー（{@link SegmentJooqMapper}）を介したドメインモデル変換を検証します。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SegmentPersistenceAdapter 単体テスト")
class SegmentPersistenceAdapterTest {

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private DSLContext dsl;

  @Mock
  private SegmentJooqMapper mapper;

  @InjectMocks
  private SegmentPersistenceAdapter adapter;

  @Nested
  @DisplayName("全件取得（fetchAll）シナリオ")
  class FetchAllScenario {

    /**
     * 【テスト観点 / 責務】 全件取得実行時、jOOQ から取得したレコードがマッパーによってドメインモデルに変換されて返却されることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>jOOQ レコードモック {@link MSegmentRecord} を用意</li>
     *   <li>マッパーの戻り値として勤務区分ドメインモデル {@link Segment} を用意</li>
     *   <li>jOOQ の {@code Result.map(RecordMapper)} でマッパーのメソッド参照（{@code mapper::toDomain}）が呼び出されるようスタブ設定</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>変換後のドメインモデルリストが返却されること</li>
     *   <li>{@code dsl.selectFrom(M_SEGMENT)} が実行されること</li>
     * </ul>
     */
    @Test
    @DisplayName("正常系: fetchAll 実行時、ID昇順で取得したレコードをドメインモデルへマッピングして返却すること")
    void shouldFetchAllSegmentsAndMapToDomain() {
      // [Given]
      MSegmentRecord record = mock(MSegmentRecord.class);
      Segment domainSegment = mock(Segment.class);
      when(mapper.toDomain(record)).thenReturn(domainSegment);

      // jOOQ の Result.map(RecordMapper) に対して、渡された RecordMapper (mapper::toDomain) を実行させる
      when(dsl.selectFrom(M_SEGMENT)
          .orderBy(M_SEGMENT.ID.asc())
          .fetch()
          .map(ArgumentMatchers.<RecordMapper<MSegmentRecord, Segment>>any()))
          .thenAnswer(invocation -> {
            RecordMapper<MSegmentRecord, Segment> recordMapper = invocation.getArgument(0);
            return List.of(Objects.requireNonNull(recordMapper.map(record)));
          });

      clearInvocations(dsl);

      // [When]
      List<Segment> actual = adapter.fetchAll();

      // [Then]
      assertThat(actual)
          .hasSize(1)
          .containsExactly(domainSegment);

      verify(dsl).selectFrom(M_SEGMENT);
    }
  }
}
