package com.computer_rescuer.attendance_management.application.interactor;

import com.computer_rescuer.attendance_management.application.port.in.GetStampLogsUseCase;
import com.computer_rescuer.attendance_management.application.port.out.FetchStampLogPort;
import com.computer_rescuer.attendance_management.domain.model.StampLog;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * {@link GetStampLogsUseCase} の実装クラス（インターラクター）。
 * <p>
 * アプリケーションのビジネスルール（今回は単純なデータ取得と中継）を制御し、 外部システムとの通信を抽象化された出力ポート（Port）へ委譲します。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GetStampLogsInteractor implements GetStampLogsUseCase {

  private final FetchStampLogPort fetchStampLogPort;

  @Override
  public List<StampLog> getDailyLogs(LocalDate date) {
    log.info("全従業員の日次打刻履歴を取得します。対象日: {}", date);
    return fetchStampLogPort.fetchDailyLogs(date);
  }

  @Override
  public List<StampLog> getUserLogs(String employeeNumber, LocalDate fromDate, LocalDate toDate) {
    log.info("ユーザーの打刻履歴を取得します。社員番号: {}, 期間: {} 〜 {}", employeeNumber,
        fromDate,
        toDate);
    return fetchStampLogPort.fetchUserLogs(employeeNumber, fromDate, toDate);
  }
}
