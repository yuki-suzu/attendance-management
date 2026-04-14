package com.computer_rescuer.attendance_management.adapter.in.web;

import com.computer_rescuer.attendance_management.application.port.in.ExtractUnstampedUsersUseCase;
import com.computer_rescuer.attendance_management.domain.model.DailyAttendance;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 打刻漏れ（未打刻）の従業員に関する操作を提供するRESTコントローラー。
 */
@RestController
@RequestMapping("/api/v1/attendances/unstamped")
@RequiredArgsConstructor
public class UnstampedUserController {

  private final ExtractUnstampedUsersUseCase extractUnstampedUsersUseCase;

  /**
   * 指定した日付における、出勤予定時刻を過ぎて未打刻の従業員一覧を取得します。
   *
   * @param date 抽出対象の日付（未指定時はシステム日付の本日）
   * @return 未打刻者のリスト
   */
  @GetMapping
  public ResponseEntity<List<DailyAttendance>> getUnstampedUsers(
      @RequestParam(required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
  ) {
    // 日付指定がなければ「今日」を対象とする
    LocalDate targetDate = (date != null) ? date : LocalDate.now();

    List<DailyAttendance> response = extractUnstampedUsersUseCase.execute(targetDate);

    return ResponseEntity.ok(response);
  }
}
