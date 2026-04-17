package com.computer_rescuer.attendance_management.application.interactor;

import com.computer_rescuer.attendance_management.application.port.in.ExtractUnstampedUsersUseCase;
import com.computer_rescuer.attendance_management.application.port.in.GetDailyAttendanceUseCase;
import com.computer_rescuer.attendance_management.domain.model.DailyAttendance;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 未打刻者抽出ユースケースの実装クラス。
 * <p>
 * 既存の {@link GetDailyAttendanceUseCase} を再利用して全従業員の出勤状況を取得し、 ドメインモデルによって「遅刻 /
 * 打刻忘れ」と判定された従業員のみをフィルタリングして返却します。 時間比較などの業務ロジックはドメインモデルに委譲されているため、本クラスはフィルタリングのみを担当します。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExtractUnstampedUsersInteractor implements ExtractUnstampedUsersUseCase {

  /**
   * 既存の全件出勤状況取得ユースケースを再利用する
   */
  private final GetDailyAttendanceUseCase getDailyAttendanceUseCase;

  @Override
  public List<DailyAttendance> execute(LocalDate targetDate) {
    log.info("{} の未打刻者（遅刻・打刻忘れ）抽出処理を開始します。", targetDate);

    // 1. 既存のユースケースを利用して、マスタ補完・判定済みの全件リストを取得
    List<DailyAttendance> allAttendances = getDailyAttendanceUseCase.execute(targetDate);

    // 2. ドメインモデルが「LATE_OR_FORGOT」と判定した人のみを抽出
    List<DailyAttendance> unstampedUsers = allAttendances.stream()
        .filter(a -> a.status() == DailyAttendance.Status.LATE_OR_FORGOT)
        .toList();

    log.info("未打刻者を {} 名抽出しました。", unstampedUsers.size());
    return unstampedUsers;
  }
}
