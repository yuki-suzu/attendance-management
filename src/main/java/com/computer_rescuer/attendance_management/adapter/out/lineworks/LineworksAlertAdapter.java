package com.computer_rescuer.attendance_management.adapter.out.lineworks;

import com.computer_rescuer.attendance_management.adapter.out.lineworks.client.LineworksMessageApi;
import com.computer_rescuer.attendance_management.application.port.out.SendAlertPort;
import com.computer_rescuer.attendance_management.infrastructure.property.LineworksProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * アラート通知出力ポート（{@link SendAlertPort}）の LINE WORKS 実装アダプター。
 * <p>
 * Application層からの「通知してほしい」という抽象的な依頼を受け取り、 設定ファイルから LINE WORKS の管理者用 Channel ID を取得した上で、 実際の API
 * クライアント（{@link LineworksMessageApi}）へ送信処理を委譲します。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LineworksAlertAdapter implements SendAlertPort {

  private final LineworksMessageApi lineworksMessageApi;
  private final LineworksProperties properties;

  @Override
  public void send(String message) {
    // インフラの都合（宛先ID）は、Adapter層のここで初めて解決される
    String channelId = properties.alertChannelId();

    log.debug("LINE WORKS の管理者チャンネル ({}) へアラートを送信します。", channelId);

    lineworksMessageApi.sendChannelMessage(channelId, message);
  }
}
