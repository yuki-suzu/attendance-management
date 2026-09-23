package com.computer_rescuer.attendance_management.adapter.out.hrmos.client;

import com.computer_rescuer.attendance_management.adapter.out.hrmos.model.HrmosDailyWorkOutput;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;

/**
 * HRMOS 勤怠実績 API と通信を行う専用クライアント。
 */
@Component
@RequiredArgsConstructor
public class HrmosWorkOutputApi {

  private final HrmosCoreHttpClient coreClient;

  /**
   * 指定日における全従業員の日次勤怠を取得します。
   */
  public List<HrmosDailyWorkOutput> fetchDailyWorkOutputs(String token, String date, int page) {
    String path = String.format("/work_outputs/daily/%s", date);
    return coreClient.fetchAndParseList(
        token, path, page, "work_outputs", "日次勤怠", new TypeReference<>() {
        }
    );
  }

  /**
   * 指定月の特定ユーザーにおける勤怠実績一覧を取得します。
   * <p>
   * 1ユーザーあたり最大31日分のため、ページング不要（1ページ完結）で全量取得可能です。
   * </p>
   *
   * @param token  認証トークン
   * @param month  処理対象月（例: "2026-09"）
   * @param userId HRMOS ユーザーID
   * @return 当該月の勤怠実績リスト
   */
  public List<HrmosDailyWorkOutput> fetchMonthlyWorkOutputsByUser(String token, String month,
      Integer userId) {
    String path = String.format("/work_outputs/monthly/%s", month);
    Map<String, String> queryParams = Map.of("user_id", String.valueOf(userId));

    return coreClient.fetchAndParseList(
        token, path, 1, queryParams, "work_outputs", "月次勤怠", new TypeReference<>() {
        }
    );
  }
}
