package com.computer_rescuer.attendance_management.adapter.out.kafka.support;

import com.computer_rescuer.attendance_management.adapter.out.kafka.dto.UnstampedAlertEvent;
import com.computer_rescuer.attendance_management.adapter.out.kafka.dto.UnstampedDirectReminderEvent;
import com.computer_rescuer.attendance_management.domain.model.DailyAttendance;
import com.computer_rescuer.attendance_management.domain.model.Employee;
import com.computer_rescuer.attendance_management.infrastructure.property.KafkaProperties;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 未打刻アラート判定結果から各種 Kafka 送信用イベント DTO を構築するビルダー。
 * <p>
 * ドメイン層で導出された未打刻者リストを受け取り、管理者向けサマリーアラートおよび 本人向けダイレクト通知のペイロードへ変換・構築する責務を担います。<br> 本クラスは Kafka
 * 送信アダプター層に位置するため、{@link KafkaProperties} の配信設定 （本人DM送信の有効化フラグ）を直接参照してイベントパラメータを安全にカプセル化します。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UnstampedAlertEventBuilder {

  private final KafkaProperties kafkaProperties;

  /**
   * 未打刻者一覧から管理者向けサマリーアラートイベントを組み立てます。
   *
   * @param targetDate             対象日
   * @param now                    検知日時
   * @param alerts                 未打刻と判定された勤怠リスト
   * @param employeeMap            従業員マスタマップ
   * @param alreadyCheckedTodayIds 本日すでにチェック済みの従業員IDセット
   * @param monthlyCounts          当月の累積チェック回数マップ
   * @return 構築された {@link UnstampedAlertEvent}
   */
  public UnstampedAlertEvent buildManagerAlertEvent(
      LocalDate targetDate,
      LocalDateTime now,
      List<DailyAttendance> alerts,
      Map<Integer, Employee> employeeMap,
      Set<Integer> alreadyCheckedTodayIds,
      Map<Integer, Integer> monthlyCounts
  ) {
    List<UnstampedAlertEvent.UnstampedEmployee> employees = alerts.stream()
        .map(a -> {
          Employee emp = employeeMap.get(a.userId());
          String email = (emp != null) ? emp.email() : null;

          int baseCount = monthlyCounts.getOrDefault(a.userId(), 0);
          int currentCount =
              alreadyCheckedTodayIds.contains(a.userId()) ? Math.max(baseCount, 1) : baseCount + 1;

          return new UnstampedAlertEvent.UnstampedEmployee(
              a.employeeNumber(),
              email,
              a.departmentName(),
              a.fullName(),
              a.scheduledStartAt(),
              currentCount
          );
        })
        .toList();

    return new UnstampedAlertEvent(targetDate, now, employees);
  }

  /**
   * 未打刻者のうち、本日初回検知者のみを抽出して本人向け個別DMイベントを組み立てます。
   * <p>
   * アプリケーション設定（{@link KafkaProperties#directReminderEnabled()}）が無効（false）の場合は、 受信側の
   * notification-service において「管理者代理通知（送信成功）」として処理させるため、 各従業員のメールアドレスを意図して {@code null}
   * でペイロードを構築します。<br> 有効な場合は、従業員マスタに登録されているメールアドレス（未登録なら null、空文字等の不正値ならそのまま）をセットします。
   * </p>
   *
   * @param alerts                 未打刻と判定された勤怠リスト
   * @param employeeMap            従業員マスタマップ
   * @param alreadyCheckedTodayIds 本日すでにチェック済みの従業員IDセット
   * @return 構築された {@link UnstampedDirectReminderEvent}
   */
  public UnstampedDirectReminderEvent buildDirectReminderEvent(
      List<DailyAttendance> alerts,
      Map<Integer, Employee> employeeMap,
      Set<Integer> alreadyCheckedTodayIds
  ) {
    boolean directReminderEnabled = kafkaProperties.directReminderEnabled();
    if (!directReminderEnabled) {
      log.info(
          "ℹ️ 本人向けDMが無効設定のため、管理者代理送信モード（email = proxy）でイベントを生成します。");
    }

    List<UnstampedDirectReminderEvent.DirectReminderEmployee> directEmployees = alerts.stream()
        .filter(a -> !alreadyCheckedTodayIds.contains(a.userId()))
        .map(a -> {
          Employee emp = employeeMap.get(a.userId());

          // directReminderEnabled が false の時は強制的に proxy@example.com（代理送信合図）
          // true の時はマスタの値をそのまま連携（未登録なら null、空文字なら設定ミスとして伝達）
          String email = !directReminderEnabled
              ? "proxy@example.com"
              : (emp != null ? emp.email() : null);

          return new UnstampedDirectReminderEvent.DirectReminderEmployee(
              a.employeeNumber(),
              email,
              a.fullName()
          );
        })
        .toList();

    return new UnstampedDirectReminderEvent(directEmployees);
  }
}
