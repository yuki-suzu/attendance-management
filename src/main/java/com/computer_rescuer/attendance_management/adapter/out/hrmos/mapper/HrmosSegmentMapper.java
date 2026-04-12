package com.computer_rescuer.attendance_management.adapter.out.hrmos.mapper;

import com.computer_rescuer.attendance_management.adapter.out.hrmos.model.HrmosSegment;
import com.computer_rescuer.attendance_management.domain.model.Segment;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import org.mapstruct.Mapper;

/**
 * HRMOS勤務区分モデルとドメイン勤務区分モデルを相互変換するマッパー。
 */
@Mapper(componentModel = "spring")
public interface HrmosSegmentMapper {

  /**
   * HRMOSの勤務区分モデルをドメインモデルに変換します。
   *
   * @param hrmosSegment 変換元のHRMOS勤務区分モデル
   * @return 変換後の勤務区分ドメインモデル
   */
  Segment toDomain(HrmosSegment hrmosSegment);

  /**
   * HRMOSの勤務区分モデルリストをドメインモデルリストに一括変換します。
   *
   * @param hrmosSegments 変換元のリスト
   * @return 変換後のリスト
   */
  List<Segment> toDomainList(List<HrmosSegment> hrmosSegments);

  /**
   * HRMOSから連携された日時（OffsetDateTime）から、ドメインモデルで必要な時間（LocalTime）のみを抽出します。
   * <p>
   * MapStructによる自動マッピング時に、型の不一致を解消するために自動的に利用されます。
   * </p>
   *
   * @param value タイムゾーン付きの日時データ
   * @return 抽出された時間データ（入力がnullの場合はnull）
   */
  default LocalTime map(OffsetDateTime value) {
    return value == null ? null : value.toLocalTime();
  }
}
