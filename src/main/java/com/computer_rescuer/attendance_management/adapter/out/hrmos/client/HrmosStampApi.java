package com.computer_rescuer.attendance_management.adapter.out.hrmos.client;

import com.computer_rescuer.attendance_management.adapter.out.hrmos.model.HrmosStampLog;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * HRMOS 打刻ログ（Stamps）APIを呼び出すクライアント。
 */
@Component
@RequiredArgsConstructor
public class HrmosStampApi {

  private final HrmosCoreHttpClient coreClient;

  /**
   * 指定された日付の打刻ログ一覧を取得します。
   *
   * @param token API通信用の有効なアクセストークン
   * @param date  対象日 (yyyy-MM-dd)
   * @return 打刻ログモデルのリスト
   */
  public List<HrmosStampLog> fetchStampLogs(String token, String date) {
    String path = String.format("/stamp_logs/daily/%s", date);

    return coreClient.fetchAndParseList(
        token,
        path,
        1,               // stamp_logs APIは基本的に1ページで全件返却される想定
        "stamp_logs",     // JSONルートノード
        "打刻ログ",
        new TypeReference<>() {
        }
    );
  }
}
