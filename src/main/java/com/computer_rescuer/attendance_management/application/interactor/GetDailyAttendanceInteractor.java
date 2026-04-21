package com.computer_rescuer.attendance_management.application.interactor;

import static com.computer_rescuer.attendance_management.shared.DateTimeConstants.JST;

import com.computer_rescuer.attendance_management.application.port.in.GetDailyAttendanceUseCase;
import com.computer_rescuer.attendance_management.application.port.out.FetchDailyWorkRecordPort;
import com.computer_rescuer.attendance_management.application.port.out.FetchSegmentPort;
import com.computer_rescuer.attendance_management.domain.model.DailyAttendance;
import com.computer_rescuer.attendance_management.domain.model.DailyWorkRecord;
import com.computer_rescuer.attendance_management.domain.model.Segment;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 出勤状況取得ユースケースの実装クラス。
 * <p>
 * このクラスはアプリケーション層の「オーケストレーター（進行役）」として機能します。<br>
 * 外部システム（HRMOS）から取得した勤怠実績と、内部DBから取得した勤務区分マスタを結合（マージ）し、
 * ドメインモデル（{@link DailyAttendance}）に生成と判定を委譲します。<br>
 * ビジネスロジック（遅刻の定義など）は一切持たず、データの調達とドメインの呼び出しに専念します。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GetDailyAttendanceInteractor implements GetDailyAttendanceUseCase {

  private final FetchDailyWorkRecordPort fetchPort;
  private final FetchSegmentPort segmentPort;

  @Override
  public List<DailyAttendance> execute(LocalDate targetDate) {
    log.info("{} の出勤状況同期を開始します。", targetDate);

    List<DailyWorkRecord> records = fetchPort.fetchByDate(targetDate);
    List<Segment> segments = segmentPort.fetchAll();

    Map<String, Segment> segmentMap = new java.util.HashMap<>();
    for (Segment segment : segments) {
      segmentMap.putIfAbsent(segment.title(), segment);
    }

    ZonedDateTime now = ZonedDateTime.now(JST);

    return records.stream()
        // 💡 修正: APIの「申請ステータス」ではなく、マスタの「勤務区分ステータス」で休日を弾く
        .filter(r -> {
          Segment masterSegment = segmentMap.get(r.segmentTitle());

          // マスタに存在し、かつステータスが 1(勤務) 以外（休日やその他）の場合は除外する
          if (masterSegment != null && masterSegment.status() != 1) {
            return false;
          }
          // マスタに存在しないイレギュラーな名称の場合は、安全のため「出勤判定の対象」として残す
          return true;
        })
        .map(r -> {
          Segment masterSegment = segmentMap.get(r.segmentTitle());

          // マスタが存在すれば予定時刻を取得、なければ null
          LocalTime scheduledStart = (masterSegment != null) ? masterSegment.startAt() : null;

          if (masterSegment == null && r.segmentTitle() != null && !r.segmentTitle().isBlank()) {
            log.warn("⚠️ 日次勤怠の勤務区分名 '{}' がマスタ(m_segment)に存在しません。対象社員: {}",
                r.segmentTitle(), r.employeeNumber());
          }

          // 判定と生成はドメインモデルにお任せ
          return DailyAttendance.create(r, scheduledStart, targetDate, now);

        }).toList();
  }
}
