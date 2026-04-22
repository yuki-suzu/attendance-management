package com.computer_rescuer.attendance_management.adapter.in.handler;

import com.computer_rescuer.attendance_management.application.port.out.SendErrNoticePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ErrorHandler;

/**
 * スケジュールタスク（@Scheduled）で発生した未捕捉例外のグローバルエラーハンドラー。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledTaskErrorHandler implements ErrorHandler {

  private final SendErrNoticePort sendErrNoticePort;

  @Override
  public void handleError(Throwable t) {
    log.error("スケジュールタスクで予期せぬエラーが発生しました", t);

    try {
      String message = String.format(
          "🚨 【バッチ処理エラー】\n" +
              "・エラー内容: %s\n" +
              "※詳細はサーバーログを確認してください。",
          ExceptionUtils.getRootCauseMessage(t)
      );

      sendErrNoticePort.send(message);

    } catch (Exception e) {
      log.error("【二次障害】LINE WORKSへのバッチエラー通知に失敗しました。", e);
    }
  }
}
