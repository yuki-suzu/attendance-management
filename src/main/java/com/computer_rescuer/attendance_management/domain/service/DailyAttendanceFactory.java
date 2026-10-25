package com.computer_rescuer.attendance_management.domain.service;

import com.computer_rescuer.attendance_management.domain.model.DailyAttendance;
import com.computer_rescuer.attendance_management.domain.model.DailyWorkRecord;
import com.computer_rescuer.attendance_management.domain.model.Segment;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 勤怠実績データおよび各種マスタ・実績情報を統合し、出勤判定モデル（{@link DailyAttendance}）を生成するドメインファクトリ。
 * <p>
 * Interactor が個別に行っていた周辺情報のエンリッチ処理（打刻時刻の紐付け、所属部門の補完、
 * 勤務区分マスタからの出勤予定時刻の解決）を集約し、完全な状態の出勤判定オブジェクトを構築します。
 * </p>
 */
@Component
public class DailyAttendanceFactory {

  /**
   * 日次勤怠実績レコードと関連マップ群を統合し、出勤判定済みの {@link DailyAttendance} を生成します。
   *
   * @param record        外部システムから取得した生の勤怠実績データ
   * @param segmentMap    勤務区分名をキーとする勤務区分マスタのマップ
   * @param departmentMap 従業員IDをキーとする部門名マップ（未設定時は「未所属」を補完）
   * @param clockInMap    従業員IDをキーとする出勤打刻時刻マップ
   * @param targetDate    出勤判定の対象日
   * @param now           出勤判定の基準となる現在日時（タイムゾーン付き）
   * @return 出勤判定ロジックが適用された {@link DailyAttendance} インスタンス
   */
  public DailyAttendance create(
      DailyWorkRecord record,
      Map<String, Segment> segmentMap,
      Map<Integer, String> departmentMap,
      Map<Integer, LocalTime> clockInMap,
      LocalDate targetDate,
      ZonedDateTime now
  ) {
    // 1. 実績打刻時刻および部門名の補完
    LocalTime stampingTime = clockInMap.get(record.userId());
    String departmentName = departmentMap.getOrDefault(record.userId(), "未所属");

    DailyWorkRecord enrichedRecord = record
        .withStampingTime(stampingTime)
        .withDepartmentName(departmentName);

    // 2. 勤務区分マスタから出勤予定時刻を取得
    Segment segment = segmentMap.get(record.segmentTitle());
    LocalTime scheduledTime = (segment != null) ? segment.startAt() : null;

    // 3. ドメインモデルの生成とステータス自動判定
    return DailyAttendance.create(enrichedRecord, scheduledTime, targetDate, now);
  }
}
