package com.computer_rescuer.attendance_management.adapter.out.persistence.mapper;

import com.computer_rescuer.attendance_management.domain.model.Segment;
import com.computer_rescuer.attendance_management.generated.jooq.tables.records.MSegmentRecord;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 勤務区分ドメインモデルからjOOQレコードへのマッピングを担当するマッパー。
 * <p>
 * 勤務区分(M_SEGMENT)テーブル固有のデータ変換ロジックをカプセル化します。
 * </p>
 */
@Component
public class SegmentJooqMapper {

  /**
   * 勤務区分ドメインモデルのリストをjOOQレコードのリストに変換します。
   *
   * @param segments 変換対象の勤務区分ドメインモデルリスト
   * @return M_SEGMENTテーブル用のjOOQレコードリスト
   */
  public List<MSegmentRecord> toRecords(List<Segment> segments) {
    return segments.stream()
        .map(s -> {
          MSegmentRecord r = new MSegmentRecord();
          r.setId(s.id());
          r.setTitle(s.title());
          r.setDisplayTitle(s.displayTitle());
          r.setStatus(s.status());
          r.setStartAt(s.startAt());
          r.setEndAt(s.endAt());
          return r;
        })
        .toList();
  }
}
