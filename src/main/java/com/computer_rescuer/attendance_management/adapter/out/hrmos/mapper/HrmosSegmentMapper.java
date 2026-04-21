package com.computer_rescuer.attendance_management.adapter.out.hrmos.mapper;

import static com.computer_rescuer.attendance_management.shared.DateTimeConstants.JST;

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

  Segment toDomain(HrmosSegment hrmosSegment);

  List<Segment> toDomainList(List<HrmosSegment> hrmosSegments);

  /**
   * HRMOSから連携された日時（OffsetDateTime）から、日本時間（JST）における時間（LocalTime）のみを抽出します。
   * <p>
   * コンテナ環境等でUTCに正規化されてデシリアライズされた場合でも、 確実に日本時間の「時計の針（例: 09:00）」として取り扱うための補正を行います。
   * </p>
   *
   * @param value タイムゾーン付きの日時データ
   * @return 日本時間ベースの抽出された時間データ（入力がnullの場合はnull）
   */
  default LocalTime map(OffsetDateTime value) {
    if (value == null) {
      return null;
    }
    // UTC(00:00Z)になっていても、日本時間(+09:00)の視点に変換してから時間を抽出する
    return value.atZoneSameInstant(JST).toLocalTime();
  }
}
