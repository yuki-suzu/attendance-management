package com.computer_rescuer.attendance_management.application.support;

import com.computer_rescuer.attendance_management.domain.model.DailyAttendance;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * 未打刻アラートの判定結果を、外部通知用のテキストメッセージに整形するフォーマッター。
 * <p>
 * アプリケーション層のユースケース（Interactor）から利用され、 ドメインモデルを汎用的な文字列（String）に変換する責務を持ちます。
 * </p>
 */
@Component
public class UnstampedAlertMessageFormatter {

  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

  public String format(List<DailyAttendance> alerts, LocalDate date) {
    StringBuilder sb = new StringBuilder();
    sb.append("⚠️ 【未打刻アラート】\n");
    sb.append(date.toString()).append(" の出勤打刻が確認できない従業員がいます。\n\n");

    Map<String, List<DailyAttendance>> groupedAlerts = alerts.stream()
        .collect(Collectors.groupingBy(DailyAttendance::departmentName));

    groupedAlerts.forEach((departmentName, attendances) -> {
      sb.append("🏢 ").append(departmentName).append("\n");
      for (DailyAttendance attendance : attendances) {
        String timeStr = attendance.scheduledStartAt() != null
            ? attendance.scheduledStartAt().format(TIME_FORMATTER)
            : "予定不明";
        sb.append(String.format("  ・%s （予定: %s〜）%n", attendance.fullName(), timeStr));
      }
      sb.append("\n");
    });

    sb.append("※打刻漏れ、または遅刻の可能性があります。状況の確認をお願いします。");
    return sb.toString();
  }
}
