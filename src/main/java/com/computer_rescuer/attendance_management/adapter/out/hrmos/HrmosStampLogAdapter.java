package com.computer_rescuer.attendance_management.adapter.out.hrmos;

import static com.computer_rescuer.attendance_management.adapter.out.hrmos.support.HrmosPaginationHelper.fetchAllPages;
import static com.computer_rescuer.attendance_management.shared.DateTimeConstants.ISO_LOCAL_DATE;
import static com.computer_rescuer.attendance_management.shared.DateTimeConstants.ISO_OFFSET_DATE_TIME;
import static com.computer_rescuer.attendance_management.shared.DateTimeConstants.JST;

import com.computer_rescuer.attendance_management.adapter.out.hrmos.client.HrmosAuthApi;
import com.computer_rescuer.attendance_management.adapter.out.hrmos.client.HrmosStampLogApi;
import com.computer_rescuer.attendance_management.adapter.out.hrmos.mapper.HrmosStampLogMapper;
import com.computer_rescuer.attendance_management.adapter.out.hrmos.model.HrmosStampLog;
import com.computer_rescuer.attendance_management.application.port.out.FetchEmployeeByIdPort;
import com.computer_rescuer.attendance_management.application.port.out.FetchStampLogPort;
import com.computer_rescuer.attendance_management.application.port.out.ResolveHrmosUserIdPort;
import com.computer_rescuer.attendance_management.domain.model.Employee;
import com.computer_rescuer.attendance_management.domain.model.StampLog;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * HRMOSから打刻ログ実績を取得する統合アダプター。
 * <p>
 * 外部API（HRMOS）から取得した生データと、ローカルDB（m_employee）の従業員情報を 結合（エンリッチ）し、完全なドメインモデルへ変換して返却します。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HrmosStampLogAdapter implements FetchStampLogPort {

  private final HrmosAuthApi authApi;
  private final HrmosStampLogApi stampLogApi;
  private final HrmosStampLogMapper mapper;
  private final FetchEmployeeByIdPort employeeByIdPort;
  private final ResolveHrmosUserIdPort resolveHrmosUserIdPort;

  @Override
  public List<StampLog> fetchDailyLogs(LocalDate date) {
    if (date == null) {
      return List.of();
    }

    String token = authApi.fetchToken();
    String dateStr = date.format(ISO_LOCAL_DATE);
    List<HrmosStampLog> allRawData = new ArrayList<>();

    int page = 1;
    while (true) {
      List<HrmosStampLog> paged = stampLogApi.fetchDailyStampLogs(token, dateStr, page);
      if (paged == null || paged.isEmpty()) {
        break;
      }

      allRawData.addAll(paged);
      log.debug("🔍 HRMOSから日次打刻ログを {} 件取得しました (page: {})", paged.size(), page);

      if (paged.size() < 100) {
        break;
      }
      page++;
    }

    // 取得した生データをローカルDBの社員情報でエンリッチして返す
    return enrichWithEmployeeDataAndMap(allRawData);
  }

  @Override
  public List<StampLog> fetchUserLogs(String employeeNumber, LocalDate fromDate, LocalDate toDate) {
    if (!StringUtils.hasText(employeeNumber)) {
      return List.of();
    }

    // 💡 Optional の機能を使って、ID解決とエラーハンドリングを1つのチェーンにまとめる
    return resolveHrmosUserIdPort.resolve(employeeNumber)
        .map(userId -> fetchLogsFromApi(userId, fromDate, toDate)) // 値があればAPIを叩く
        .orElseGet(() -> { // 値がなければエラーログを出して空リストを返す
          log.error("社員番号 '{}' に該当するユーザーが存在しないか、HRMOSと連携されていません。",
              employeeNumber);
          return List.of();
        });
  }

  /**
   * HRMOS API から指定されたユーザーの打刻ログを取得し、エンリッチして返却します。 (fetchUserLogs の後半部分を別メソッドに切り出し)
   */
  private List<StampLog> fetchLogsFromApi(Integer userId, LocalDate fromDate, LocalDate toDate) {
    String token = authApi.fetchToken();
    String fromApiString = (fromDate != null)
        ? fromDate.atStartOfDay(JST).format(ISO_OFFSET_DATE_TIME)
        : null;
    String toApiString = (toDate != null)
        ? toDate.plusDays(1).atStartOfDay(JST).format(ISO_OFFSET_DATE_TIME)
        : null;

    List<HrmosStampLog> allRawData = fetchAllPages("打刻ログ", page ->
        stampLogApi.fetchUserStampLogs(token, userId, fromApiString, toApiString, page)
    );

    return enrichWithEmployeeDataAndMap(allRawData);
  }

  /**
   * HRMOSの生データリストから重複のないuserIdを抽出し、ローカルDBの従業員情報と結合してドメインモデルへ変換します。
   *
   * @param rawDataList HRMOSから取得した生の打刻ログリスト
   * @return エンリッチされたドメインモデルのリスト
   */
  /**
   * HRMOSの生データリストから重複のないuserIdを抽出し、ローカルDBの従業員情報と結合してドメインモデルへ変換します。
   */
  private List<StampLog> enrichWithEmployeeDataAndMap(List<HrmosStampLog> rawDataList) {
    if (CollectionUtils.isEmpty(rawDataList)) {
      return List.of();
    }

    // 1. 全生データから、一意な userId のリストを抽出
    List<Integer> userIds = rawDataList.stream()
        .map(HrmosStampLog::userId)
        .distinct()
        .toList();

    // 2. ローカルDBから、該当する Employee 情報を一括取得 (N+1回避)
    Map<Integer, Employee> employeeMap = employeeByIdPort.fetchEmployeeMapByUserIds(userIds);

    // 3. マッピング処理自体はMapperに丸投げ！Adapterは「データを揃えて渡すだけ」に徹する
    return mapper.toDomainList(rawDataList, employeeMap);
  }
}
