package com.computer_rescuer.attendance_management.application.interactor;

import com.computer_rescuer.attendance_management.application.port.in.ExtractUnstampedUsersUseCase;
import com.computer_rescuer.attendance_management.application.port.out.SendAlertPort;
import com.computer_rescuer.attendance_management.domain.model.DailyAttendance;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 未打刻者（遅刻者）のアラート通知を実行するバッチ・ユースケース。
 * <p>
 * このクラスは、以下のオーケストレーションのみを担当します：<br> 1. {@link ExtractUnstampedUsersUseCase}
 * を呼び出し、未打刻の従業員リストを抽出。<br> 2. 抽出結果が1件以上ある場合のみ、管理者向けレポート（文字列）を組み立て。<br> 3. {@link SendAlertPort}
 * を通じて外部へ通知を依頼。<br> 通信手段（LINE WORKS等）や宛先の詳細は、Adapter層に完全に隠蔽されています。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyUnstampedAlertInteractor {

  private final ExtractUnstampedUsersUseCase extractUseCase;
  private final SendAlertPort sendAlertPort; // 👈 直接APIを呼ばず、Portに依存する！

  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern(
      "yyyy年MM月dd日");

  /**
   * 指定された日付の未打刻者を抽出し、管理者へ通知します。
   */
  public void execute(LocalDate targetDate) {
    log.info("{} の未打刻アラートバッチを開始します。", targetDate);

    List<DailyAttendance> unstampedUsers = extractUseCase.execute(targetDate);

    if (unstampedUsers.isEmpty()) {
      log.info("本日の未打刻者は0名でした。通知をスキップします。");
      return;
    }

    String message = buildAlertMessage(targetDate, unstampedUsers);

    // 👈 宛先IDすら知らない。「送ってくれ！」とポートに投げるだけ。
    sendAlertPort.send(message);

    log.info("未打刻アラート（{}名）の送信依頼を完了しました。", unstampedUsers.size());
  }

  private String buildAlertMessage(LocalDate date, List<DailyAttendance> users) {
    StringBuilder sb = new StringBuilder();
    sb.append("⚠️ 【未打刻アラート】\n");
    sb.append(date.format(DATE_FORMAT))
        .append(" の出勤予定時刻を過ぎていますが、以下のメンバーの打刻が確認できません。\n\n");

    for (DailyAttendance user : users) {
      sb.append("・").append(user.fullName())
          .append(" （社員番号: ").append(user.employeeNumber()).append("）\n")
          .append("　予定: ").append(user.scheduledStartAt())
          .append(" [").append(user.segmentTitle()).append("]\n");
    }

    sb.append("\n各担当マネージャーは状況の確認をお願いします。");

    return sb.toString();
  }
}
