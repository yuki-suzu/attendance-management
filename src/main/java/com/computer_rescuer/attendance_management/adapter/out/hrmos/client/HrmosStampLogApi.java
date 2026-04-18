package com.computer_rescuer.attendance_management.adapter.out.hrmos.client;

import com.computer_rescuer.attendance_management.adapter.out.hrmos.model.HrmosStampLog;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * HRMOS の「打刻履歴 API (/stamp_logs)」と通信を行うための専用クライアント。
 * <p>
 * このクラスは特定のドメインリソース（打刻ログ）に特化したエンドポイントパスの構築と、 HRMOSコアエンジン（{@link HrmosCoreHttpClient}）への委譲を担当します。
 * </p>
 */
@Component
@RequiredArgsConstructor
public class HrmosStampLogApi {

  private final HrmosCoreHttpClient coreClient;

  /**
   * 指定された日付における全従業員の打刻ログをページ指定で取得します。
   *
   * @param token 認証トークン
   * @param date  対象日（例: "2026-04-18"）
   * @param page  取得ページ番号
   * @return 指定ページの打刻ログ生データリスト
   */
  public List<HrmosStampLog> fetchDailyStampLogs(String token, String date, int page) {
    String path = String.format("/stamp_logs/daily/%s", date);
    return coreClient.fetchAndParseList(
        token, path, page, "stamp_logs", "日次打刻ログ", new TypeReference<>() {
        }
    );
  }

  /**
   * 指定されたユーザーIDの、特定の期間内における打刻ログをページ指定で取得します。
   * <p>
   * 二重URLエンコード（%3F など）を防ぐため、期間指定パラメータ（from, to）は パス文字列に直接結合せず、Mapに格納してコアエンジンに引き渡します。
   * </p>
   *
   * @param token  認証トークン
   * @param userId HRMOSシステム内のユーザーID
   * @param from   抽出開始日時（ISO-8601 OffsetDateTime形式）
   * @param to     抽出終了日時（ISO-8601 OffsetDateTime形式）
   * @param page   取得ページ番号
   * @return 指定ページのユーザー打刻ログ生データリスト
   */
  public List<HrmosStampLog> fetchUserStampLogs(String token, Integer userId, String from,
      String to, int page) {
    // クエリ文字列（?from=...）を含めない、純粋なエンドポイントパス
    String purePath = String.format("/stamp_logs/user/%d", userId);

    // UriBuilderに安全にエンコードさせるためのクエリパラメータマップ
    Map<String, String> queryParams = new HashMap<>();
    if (from != null) {
      queryParams.put("from", from);
    }
    if (to != null) {
      queryParams.put("to", to);
    }

    return coreClient.fetchAndParseList(
        token, purePath, page, queryParams, "stamp_logs", "ユーザー打刻ログ",
        new TypeReference<>() {
        }
    );
  }
}
