package com.computer_rescuer.attendance_management.adapter.out.hrmos.client;

import com.computer_rescuer.attendance_management.adapter.out.exception.ExternalIntegrationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * HRMOS 認証系（Authentication）APIを呼び出すクライアント。
 * <p>
 * エンドポイント: /authentication
 * </p>
 */
@Component
@RequiredArgsConstructor
public class HrmosAuthApi {

  private final HrmosCoreHttpClient coreClient;

  /**
   * API通信用のアクセストークンを取得します。
   *
   * @return アクセストークン文字列
   * @throws ExternalIntegrationException 認証に失敗した場合
   */
  public String fetchToken() {
    return coreClient.fetchToken();
  }
}
