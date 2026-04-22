package com.computer_rescuer.attendance_management.adapter.out.lineworks;

import com.computer_rescuer.attendance_management.adapter.out.lineworks.client.LineworksMessageApi;
import com.computer_rescuer.attendance_management.application.port.out.SendErrNoticePort;
import com.computer_rescuer.attendance_management.infrastructure.property.LineworksProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * アプリエラー通知出力ポート（{@link SendErrNoticePort}）の LINE WORKS 実装アダプター。
 * <p>
 * Application層からの「通知してほしい」という抽象的な依頼を受け取り、 設定ファイルからアプリ管理者の LINE WORKS ID を取得した上で、 実際の API
 * クライアント（{@link LineworksMessageApi}）へ送信処理を委譲します。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LineworksErrNoticeAdapter implements SendErrNoticePort {

  private final LineworksMessageApi lineworksMessageApi;
  private final LineworksProperties properties;

  @Override
  public void send(String message) {
    String channelId = properties.systemManagerId();

    log.debug("担当者 {} のLINE WORKS へメッセージを送信します。", channelId);

    lineworksMessageApi.sendTextMessage(channelId, message);
  }
}
