package com.computer_rescuer.attendance_management.adapter.out.hrmos.client;

import com.computer_rescuer.attendance_management.adapter.out.hrmos.model.HrmosSegment;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * HRMOS 勤務区分（Segments）APIを呼び出すクライアント。
 * <p>
 * エンドポイント: /segments
 * </p>
 */
@Component
@RequiredArgsConstructor
public class HrmosSegmentApi {

  private final HrmosCoreHttpClient coreClient;

  /**
   * HRMOSからすべての勤務区分情報を取得します。
   *
   * @param token API通信用の有効なアクセストークン
   * @return 勤務区分モデルのリスト
   */
  public List<HrmosSegment> fetchSegments(String token) {
    return coreClient.fetchAndParseList(
        token, "/segments", "segments", "勤務区分", new TypeReference<>() {
        }
    );
  }
}
