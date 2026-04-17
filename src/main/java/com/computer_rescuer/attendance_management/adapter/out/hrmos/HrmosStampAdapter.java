package com.computer_rescuer.attendance_management.adapter.out.hrmos;

import com.computer_rescuer.attendance_management.adapter.out.hrmos.client.HrmosAuthApi;
import com.computer_rescuer.attendance_management.adapter.out.hrmos.client.HrmosStampApi;
import com.computer_rescuer.attendance_management.adapter.out.hrmos.model.HrmosStampLog;
import com.computer_rescuer.attendance_management.application.port.out.FetchHrmosStampPort;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * HRMOSから生の打刻ログを取得するアダプター。
 * <p>
 * 日次勤怠API（WorkOutputs）の同期ラグを回避するため、 打刻イベントそのものを保持する StampLogs エンドポイントからデータを取得します。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HrmosStampAdapter implements FetchHrmosStampPort {

  private final HrmosAuthApi authApi;
  private final HrmosStampApi stampApi;

  /**
   * 指定された日付の打刻ログをHRMOSから取得します。
   *
   * @param date 対象日
   * @return 取得された打刻ログのリスト
   */
  @Override
  public List<HrmosStampLog> fetchByDate(LocalDate date) {
    log.debug("HRMOSから打刻ログを取得します。対象日: {}", date);

    // 1. 共通のAuthApiを使用してトークンを取得
    String token = authApi.fetchToken();

    // 2. 打刻ログAPIを呼び出す
    // 日次勤怠APIと異なり、通常1日の打刻数は全社員分でも1ページに収まるため、
    // ここではページネーションを考慮せず 1 ページ目のみを取得する仕様とします。
    // (必要に応じて HrmosDailyWorkRecordAdapter のようなループ構造へ拡張可能です)
    List<HrmosStampLog> stampLogs = stampApi.fetchStampLogs(token, date.toString());

    if (stampLogs == null) {
      log.warn("HRMOSからの打刻ログ取得結果がnullです。対象日: {}", date);
      return List.of();
    }

    log.info("HRMOSから {} 件の打刻ログを取得しました。対象日: {}", stampLogs.size(), date);
    return stampLogs;
  }
}
