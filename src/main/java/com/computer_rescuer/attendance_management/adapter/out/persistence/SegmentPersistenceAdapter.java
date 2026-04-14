package com.computer_rescuer.attendance_management.adapter.out.persistence;

import static com.computer_rescuer.attendance_management.generated.jooq.Tables.M_SEGMENT;

import com.computer_rescuer.attendance_management.adapter.out.persistence.mapper.SegmentJooqMapper;
import com.computer_rescuer.attendance_management.application.port.out.FetchSegmentPort;
import com.computer_rescuer.attendance_management.domain.model.Segment;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SegmentPersistenceAdapter implements FetchSegmentPort {

  private final DSLContext dsl;
  private final SegmentJooqMapper mapper;

  @Override
  public List<Segment> fetchAll() {
    return dsl.selectFrom(M_SEGMENT)
        .fetch()
        .map(mapper::toDomain);
  }
}