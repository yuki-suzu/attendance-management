package com.computer_rescuer.attendance_management.application.service;

import com.computer_rescuer.attendance_management.adapter.out.hrmos.model.HrmosDailyWorkOutput;
import com.computer_rescuer.attendance_management.application.port.out.MonthlyAttendanceSummaryRepositoryPort.SummaryItem;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * HRMOS の月次勤怠実績から集約日数を計算する集計ドメインサービス。
 */
@Component
public class MonthlyAttendanceAggregator {

  public static final int SEG_SCHEDULED_HOLIDAY = 2;   // 予定休
  public static final int SEG_UNSCHEDULED_HOLIDAY = 5; // 当日休
  public static final int SEG_AM_HALF_HOLIDAY = 6;     // 今前半休
  public static final int SEG_PM_HALF_HOLIDAY = 34;    // 今後半休
  public static final int SEG_DELAY = 35;              // 遅延

  private static final Map<Integer, String> TARGET_SEGMENTS = Map.of(
      SEG_SCHEDULED_HOLIDAY, "予定休",
      SEG_UNSCHEDULED_HOLIDAY, "当日休",
      SEG_AM_HALF_HOLIDAY, "午前半休",
      SEG_PM_HALF_HOLIDAY, "午後半休",
      SEG_DELAY, "遅延"
  );

  /**
   * HRMOS の月次勤怠データから当月集計アイテムを構築します。
   * <p>
   * 1. 処理日より未来の実績は集計から除外します。<br> 2. 当日休・半休・遅延が 1 件も存在しない（予定休のみ等）場合は、空リストを返却して登録不要と判定します。
   * </p>
   *
   * @param dailyOutputs HRMOSから取得した日次勤怠実績リスト
   * @param today        処理当日（集計の上限日）
   * @return 永続化対象のサマリ項目リスト（登録対象外の場合は空リスト）
   */
  public List<SummaryItem> aggregate(List<HrmosDailyWorkOutput> dailyOutputs, LocalDate today) {
    if (dailyOutputs == null || dailyOutputs.isEmpty()) {
      return List.of();
    }

    // 今日以前の実績のみを対象とし、タイトル別に日数を集計
    Map<String, Long> titleCounts = dailyOutputs.stream()
        .filter(out -> {
          LocalDate day = LocalDate.parse(out.day());
          return !day.isAfter(today);
        })
        .filter(out -> out.segmentTitle() != null)
        .collect(Collectors.groupingBy(HrmosDailyWorkOutput::segmentTitle, Collectors.counting()));

    // 不定期区分（当日休、半休、遅延）の合計件数をチェック
    long irregularCount = titleCounts.entrySet().stream()
        .filter(e -> {
          String title = e.getKey();
          return title.contains(TARGET_SEGMENTS.get(SEG_UNSCHEDULED_HOLIDAY))
              || title.contains(TARGET_SEGMENTS.get(SEG_AM_HALF_HOLIDAY))
              || title.contains(TARGET_SEGMENTS.get(SEG_PM_HALF_HOLIDAY))
              || title.contains(TARGET_SEGMENTS.get(SEG_DELAY));
        })
        .mapToLong(Map.Entry::getValue)
        .sum();

    // 💡 予定休しかない（不定期区分が0件）場合は登録不要！
    if (irregularCount == 0) {
      return List.of();
    }

    List<SummaryItem> items = new ArrayList<>();
    for (var entry : TARGET_SEGMENTS.entrySet()) {
      int segId = entry.getKey();
      String title = entry.getValue();

      int count = titleCounts.entrySet().stream()
          .filter(e -> e.getKey().contains(title))
          .mapToInt(e -> e.getValue().intValue())
          .sum();

      if (count > 0) {
        // 予定休は常に '1'、それ以外は '0' で登録
        String msgSend = (segId == SEG_SCHEDULED_HOLIDAY) ? "1" : "0";
        items.add(new SummaryItem(segId, title, count, msgSend));
      }
    }

    return items;
  }
}
