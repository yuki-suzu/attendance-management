package com.computer_rescuer.attendance_management.adapter.in.handler;

import com.computer_rescuer.attendance_management.adapter.in.model.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Controllerが返却した ApiResponse を横取りし、 JSONに変換される直前に messages.properties から文言を注入するアドバイスクラス。
 */
@RestControllerAdvice
@RequiredArgsConstructor
public class ApiResponseAdvice implements ResponseBodyAdvice<Object> {
  
  private final MessageSource messageSource;

  @Override
  public boolean supports(MethodParameter returnType,
      Class<? extends HttpMessageConverter<?>> converterType) {
    // 戻り値の型が ApiResponse の場合のみ beforeBodyWrite を実行する
    return ApiResponse.class.isAssignableFrom(returnType.getParameterType());
  }

  @Override
  public Object beforeBodyWrite(Object body, MethodParameter returnType,
      MediaType selectedContentType,
      Class<? extends HttpMessageConverter<?>> selectedConverterType,
      ServerHttpRequest request, ServerHttpResponse response) {

    // Java 16+ のパターンマッチングで ApiResponse にキャスト
    if (body instanceof ApiResponse<?> apiResponse) {

      // LocaleContextHolder を使うと、リクエストの言語設定(Accept-Language)を自動解決してくれます
      String resolvedMessage = messageSource.getMessage(
          apiResponse.code(),
          null,
          LocaleContextHolder.getLocale()
      );

      // Recordは不変（イミュータブル）なので、メッセージを埋めた新しいRecordを作ってすり替える
      return new ApiResponse<>(
          apiResponse.code(),
          resolvedMessage, // 👈 抽出したメッセージをセット！
          apiResponse.detail(),
          apiResponse.data()
      );
    }

    return body;
  }
}
