package com.computer_rescuer.attendance_management.adapter.out.persistence.mapper;

import com.computer_rescuer.attendance_management.domain.model.Segment;
import com.computer_rescuer.attendance_management.generated.jooq.tables.records.MSegmentRecord;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 勤務区分ドメインモデルとjOOQレコードの相互変換を担当するマッパー。
 */
@Component
public class SegmentJooqMapper {

  /**
   * jOOQレコードをドメインモデルに変換します。
   */
  public Segment toDomain(MSegmentRecord r) {
    return new Segment(
        r.getId(),
        r.getTitle(),
        r.getDisplayTitle(),
        r.getStatus(),
        r.getStartAt(),
        r.getEndAt()
    );
  }

  /**
   * ドメインモデルをjOOQレコードに変換します（既存メソッド）。
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
