package com.computer_rescuer.attendance_management.application.port.out;

import com.computer_rescuer.attendance_management.domain.model.DailyAttendance;
import java.util.List;

/**
 * 未打刻アラートを外部システムへ送信するための出力ポート。
 * <p>
 * 判定結果（ドメインモデル）を、LINE WORKS 等の具体的な外部通知手段へ 受け渡すための抽象化インターフェースです。
 * </p>
 */
public interface NotifyUnstampedAlertPort {

  /**
   * 判定された未打刻アラートの一覧を外部へ通知します。
   *
   * @param alerts 通知対象となる出勤判定結果のリスト
   */
  void sendAlert(List<DailyAttendance> alerts);
}
