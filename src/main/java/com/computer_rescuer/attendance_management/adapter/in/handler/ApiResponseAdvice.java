package com.computer_rescuer.attendance_management.adapter.in.handler;

import com.computer_rescuer.attendance_management.adapter.in.model.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * コントローラーが返却した {@link ApiResponse} をインターセプトし、国際化メッセージを注入するレスポンスアドバイス。
 * <p>
 * アーキテクチャ上の Inbound Adapter（Web層）として動作し、レスポンスが JSON 等へシリアライズされる直前に {@link MessageSource}
 * からメッセージコードに対応する文言を解決して {@link ApiResponse} へ再設定します。<br>
 * 不正なコード値やメッセージ定義の欠落に対しては、フォールバックによる隠蔽を行わず即座に例外をスローする フェイルファスト（Fail-Fast）原則に従って設計されています。
 * </p>
 */
@RestControllerAdvice
@RequiredArgsConstructor
public class ApiResponseAdvice implements ResponseBodyAdvice<Object> {

  private final MessageSource messageSource;

  /**
   * インターセプト対象の戻り値型であるかを判定します。
   *
   * @param returnType    コントローラーメソッドの戻り値型メタデータ
   * @param converterType 選択された HTTP メッセージコンバーターの型
   * @return 戻り値の型が {@link ApiResponse} に代入可能である場合は {@code true}、それ以外は {@code false}
   */
  @Override
  public boolean supports(
      @NonNull MethodParameter returnType,
      @NonNull Class<? extends HttpMessageConverter<?>> converterType
  ) {
    return ApiResponse.class.isAssignableFrom(returnType.getParameterType());
  }

  /**
   * HTTP レスポンスボディの書き込み直前に介入し、{@link ApiResponse} へ解決済みメッセージを注入します。
   *
   * @param body                  コントローラーが返却した生レスポンスボディ（null の場合あり）
   * @param returnType            コントローラーメソッドの戻り値型メタデータ
   * @param selectedContentType   ネゴシエーションされた Content-Type
   * @param selectedConverterType 書き込みに使用されるメッセージコンバーターの型
   * @param request               現在の HTTP リクエスト
   * @param response              現在の HTTP レスポンス
   * @return メッセージが注入された新しい {@link ApiResponse}、または対象外の場合は元の body
   * @throws IllegalArgumentException {@link ApiResponse} の code が null または空白の場合
   * @throws NoSuchMessageException   指定された code に対応する文言が {@link MessageSource} に定義されていない場合
   */
  @Override
  @Nullable
  public Object beforeBodyWrite(
      @Nullable Object body,
      @NonNull MethodParameter returnType,
      @NonNull MediaType selectedContentType,
      @NonNull Class<? extends HttpMessageConverter<?>> selectedConverterType,
      @NonNull ServerHttpRequest request,
      @NonNull ServerHttpResponse response
  ) {
    // ApiResponse 以外のオブジェクトや null は手を加えずにそのまま通過
    if (!(body instanceof ApiResponse<?> apiResponse)) {
      return body;
    }

    String code = apiResponse.code();
    if (code == null || code.isBlank()) {
      throw new IllegalArgumentException(
          "ApiResponse の code は必須であり、空文字であってはなりません。");
    }

    // 未定義キーの場合は NoSuchMessageException をスローさせ、設定漏れ・実装ミスを早期検知（Fail-Fast）する
    String resolvedMessage = messageSource.getMessage(
        code,
        null,
        LocaleContextHolder.getLocale()
    );

    return new ApiResponse<>(
        apiResponse.code(),
        resolvedMessage,
        apiResponse.detail(),
        apiResponse.data()
    );
  }
}
