package com.computer_rescuer.attendance_management.adapter.in.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.computer_rescuer.attendance_management.adapter.in.model.ApiResponse;
import com.computer_rescuer.attendance_management.adapter.in.model.ResultCode;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;

/**
 * {@link ApiResponseAdvice} の単体テストクラス。
 * <p>
 * Inbound Web アダプター層におけるレスポンスインターセプターとして、以下の責務と境界条件を網羅的に検証します:
 * <ul>
 *   <li>戻り値の型が {@link ApiResponse} に代入可能であるかどうかのサポート判定（{@code supports}）</li>
 *   <li>レスポンスボディが null または {@link ApiResponse} 以外のオブジェクトである場合の透過パス（非介入）</li>
 *   <li>{@link ApiResponse} の {@code code} に対する入力バリデーション（null、空文字、空白文字時の Fail-Fast 例外スロー）</li>
 *   <li>{@link MessageSource} を介した現在のロケールに基づく国際化メッセージの解決と注入</li>
 *   <li>メッセージ定義欠落時における {@link NoSuchMessageException} の伝播検証</li>
 * </ul>
 * Spring Boot 4 / Java 25 環境に対応し、非推奨 API を一切含まないクリーンな単体テストとして実行されます。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ApiResponseAdvice 単体テスト")
class ApiResponseAdviceTest {

  @Mock
  private MessageSource messageSource;

  @InjectMocks
  private ApiResponseAdvice advice;

  /**
   * テスト検証用の標準 HTTP メッセージコンバーター型定義。 非推奨（Deprecated）となった MappingJackson2HttpMessageConverter
   * を避け、Spring 標準のコンバーターを使用します。
   */
  private final Class<? extends HttpMessageConverter<?>> converterType =
      StringHttpMessageConverter.class;

  private final MediaType mediaType = MediaType.APPLICATION_JSON;

  @Mock
  private MethodParameter methodParameter;

  @Mock
  private ServerHttpRequest serverHttpRequest;

  @Mock
  private ServerHttpResponse serverHttpResponse;

  @BeforeEach
  void setUp() {
    LocaleContextHolder.setLocale(Locale.JAPAN);
  }

  @AfterEach
  void tearDown() {
    LocaleContextHolder.resetLocaleContext();
  }

  @Nested
  @DisplayName("supports メソッドの型判定シナリオ")
  class SupportsScenario {

    /**
     * 【テスト観点 / 責務】 コントローラーの戻り値型が {@link ApiResponse} である場合に、 アドバイスのインターセプト対象として {@code true}
     * を返却することを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>{@link MethodParameter#getParameterType()} が {@code ApiResponse.class} を返却するよう設定</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>{@code supports} メソッドの戻り値が {@code true} であること</li>
     * </ul>
     */
    @Test
    @DisplayName("戻り値型が ApiResponse の場合、true を返却すること")
    void shouldReturnTrueWhenReturnTypeIsApiResponse() {
      // [Given]
      doReturn(ApiResponse.class).when(methodParameter).getParameterType();

      // [When]
      boolean actual = advice.supports(methodParameter, converterType);

      // [Then]
      assertThat(actual).isTrue();
    }

    /**
     * 【テスト観点 / 責務】 コントローラーの戻り値型が {@link ApiResponse} 以外（例: {@link String} や {@link Void}）の場合に、
     * インターセプト対象外として {@code false} を返却することを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>{@link MethodParameter#getParameterType()} が {@code String.class} を返却するよう設定</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>{@code supports} メソッドの戻り値が {@code false} であること</li>
     * </ul>
     */
    @Test
    @DisplayName("戻り値型が ApiResponse 以外の場合、false を返却すること")
    void shouldReturnFalseWhenReturnTypeIsNotApiResponse() {
      // [Given]
      doReturn(String.class).when(methodParameter).getParameterType();

      // [When]
      boolean actual = advice.supports(methodParameter, converterType);

      // [Then]
      assertThat(actual).isFalse();
    }
  }

  @Nested
  @DisplayName("beforeBodyWrite 非介入・透過パスシナリオ")
  class PassThroughScenario {

    /**
     * 【テスト観点 / 責務】 レスポンスボディが {@code null} の場合、何の手も加えずにそのまま {@code null} を返却し、
     * メッセージ解決等の後続処理が行われないことを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>{@code body} に {@code null} を渡す</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>戻り値が {@code null} であること</li>
     *   <li>{@link MessageSource} が一切呼び出されないこと</li>
     * </ul>
     */
    @Test
    @DisplayName("境界値: レスポンスボディが null の場合、何も手を加えずに null を返却すること")
    void shouldReturnNullWhenBodyIsNull() {
      // [When]
      Object actual = advice.beforeBodyWrite(
          null,
          methodParameter,
          mediaType,
          converterType,
          serverHttpRequest,
          serverHttpResponse
      );

      // [Then]
      assertThat(actual).isNull();
      verifyNoInteractions(messageSource);
    }

    /**
     * 【テスト観点 / 責務】 レスポンスボディが {@link ApiResponse} 以外のオブジェクト（例: 生の文字列など）の場合、
     * インスタンスを改変せずそのまま返却することを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>{@code body} に文字列インスタンス（{@code "raw response text"}）を渡す</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>渡されたオブジェクトと同一のインスタンスが返却されること</li>
     *   <li>{@link MessageSource} が一切呼び出されないこと</li>
     * </ul>
     */
    @Test
    @DisplayName("境界値: レスポンスボディが ApiResponse 以外の型の場合、改変せずそのまま返却すること")
    void shouldReturnOriginalBodyWhenBodyIsNotApiResponse() {
      // [Given]
      String rawBody = "raw response text";

      // [When]
      Object actual = advice.beforeBodyWrite(
          rawBody,
          methodParameter,
          mediaType,
          converterType,
          serverHttpRequest,
          serverHttpResponse
      );

      // [Then]
      assertThat(actual).isSameAs(rawBody);
      verifyNoInteractions(messageSource);
    }
  }

  @Nested
  @DisplayName("beforeBodyWrite 入力バリデーションシナリオ")
  class ValidationScenario {

    /**
     * 【テスト観点 / 責務】 {@link ApiResponse} の {@code code} が {@code null}、空文字、または空白文字の場合に、
     * 設計原則（Fail-Fast）に従って即座に {@link IllegalArgumentException} をスローすることを確認します（C1 分岐網羅）。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>{@code code} に {@code null}、空文字 {@code ""}、または空白文字列（半角スペース、タブ、改行）を設定した {@link ApiResponse} を渡す</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>{@link IllegalArgumentException} がスローされること</li>
     *   <li>例外メッセージに "ApiResponse の code は必須であり、空文字であってはなりません。" が含まれること</li>
     *   <li>{@link MessageSource} が一切呼び出されないこと</li>
     * </ul>
     *
     * @param blankCode テスト対象の不正なコード文字列
     */
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("異常系: ApiResponse の code が null または空白の場合、IllegalArgumentException をスローすること")
    void shouldThrowIllegalArgumentExceptionWhenCodeIsBlank(String blankCode) {
      // [Given]
      ApiResponse<String> invalidResponse = new ApiResponse<>(blankCode, null, null, "test-data");

      // [When & Then]
      assertThatThrownBy(() -> advice.beforeBodyWrite(
          invalidResponse,
          methodParameter,
          mediaType,
          converterType,
          serverHttpRequest,
          serverHttpResponse
      ))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("ApiResponse の code は必須であり、空文字であってはなりません。");

      verifyNoInteractions(messageSource);
    }
  }

  @Nested
  @DisplayName("beforeBodyWrite メッセージ解決および注入シナリオ")
  class MessageResolutionScenario {

    /**
     * 【テスト観点 / 責務】 正常系検証。有効な {@code code} を持つ {@link ApiResponse} が渡された際、 {@link MessageSource}
     * から現在のロケールに基づいたメッセージが解決され、 元のデータ構造（code, detail, data）を完全に維持した新しい {@link ApiResponse}
     * が生成されることを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>{@code code} = {@code "SUCCESS"}、detail 2件、data にリストを持つ {@link ApiResponse}</li>
     *   <li>{@link MessageSource#getMessage(String, Object[], Locale)} が "処理が正常に完了しました。" を返却するようモック設定</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>返却されたオブジェクトが {@link ApiResponse} インスタンスであること</li>
     *   <li>{@code code()} が元の "SUCCESS" と一致すること</li>
     *   <li>{@code message()} に解決されたメッセージが正しく注入されていること</li>
     *   <li>{@code detail()} の要素（キーおよびメッセージ）が完全に保持されていること</li>
     *   <li>{@code data()} のペイロードが改変されずに保持されていること</li>
     * </ul>
     */
    @Test
    @DisplayName("正常系: 有効な code に対し、MessageSource から解決されたメッセージが注入された ApiResponse を返却すること")
    void shouldInjectResolvedMessageIntoApiResponse() {
      // [Given]
      List<ApiResponse.Detail> details = List.of(
          new ApiResponse.Detail("field1", "エラー1"),
          new ApiResponse.Detail("field2", "エラー2")
      );
      List<String> payload = List.of("data-item-A", "data-item-B");
      ApiResponse<List<String>> originalResponse = new ApiResponse<>(
          ResultCode.SUCCESS.name(),
          null, // メッセージは未設定
          details,
          payload
      );

      String expectedResolvedMessage = "処理が正常に完了しました。";
      when(messageSource.getMessage(
          eq(ResultCode.SUCCESS.name()),
          isNull(),
          eq(Locale.JAPAN)
      )).thenReturn(expectedResolvedMessage);

      // [When]
      Object actual = advice.beforeBodyWrite(
          originalResponse,
          methodParameter,
          mediaType,
          converterType,
          serverHttpRequest,
          serverHttpResponse
      );

      // [Then]
      assertThat(actual)
          .isInstanceOf(ApiResponse.class);

      ApiResponse<?> actualResponse = (ApiResponse<?>) actual;
      assertThat(actualResponse.code()).isEqualTo(ResultCode.SUCCESS.name());
      assertThat(actualResponse.message()).isEqualTo(expectedResolvedMessage);
      assertThat(actualResponse.detail()).hasSize(2);
      assertThat(actualResponse.detail().getFirst().key()).isEqualTo("field1");
      assertThat(actualResponse.detail().getFirst().message()).isEqualTo("エラー1");
      assertThat(actualResponse.detail().get(1).key()).isEqualTo("field2");
      assertThat(actualResponse.detail().get(1).message()).isEqualTo("エラー2");
      assertThat(actualResponse.data()).isEqualTo(payload);

      verify(messageSource).getMessage(
          eq(ResultCode.SUCCESS.name()),
          isNull(),
          eq(Locale.JAPAN)
      );
    }

    /**
     * 【テスト観点 / 責務】 異常系（未定義キー）検証。 {@link MessageSource} に定義されていない {@code code} が渡された場合、
     * フォールバック（デフォルトメッセージの埋め込み等）によるエラー隠蔽を行わず、 {@link NoSuchMessageException}
     * をそのまま上位へスローさせる（Fail-Fast）ことを確認します。
     * <p>
     * 【事前条件（Given / Arrange）】
     * <ul>
     *   <li>{@code code} = {@code "E_UNDEFINED_CODE"} の {@link ApiResponse} を用意</li>
     *   <li>{@link MessageSource} が {@link NoSuchMessageException} をスローするようモック設定</li>
     * </ul>
     * <p>
     * 【期待結果（Then / Assert）】
     * <ul>
     *   <li>{@link NoSuchMessageException} がキャッチされずに外へ伝播すること</li>
     * </ul>
     */
    @Test
    @DisplayName("異常系: 未定義の code の場合、NoSuchMessageException がそのままスローされること（Fail-Fast）")
    void shouldPropagateNoSuchMessageExceptionWhenCodeIsNotDefined() {
      // [Given]
      String undefinedCode = "E_UNDEFINED_CODE";
      ApiResponse<Void> originalResponse = new ApiResponse<>(undefinedCode, null, null, null);

      when(messageSource.getMessage(
          eq(undefinedCode),
          isNull(),
          eq(Locale.JAPAN)
      )).thenThrow(new NoSuchMessageException(undefinedCode, Locale.JAPAN));

      // [When & Then]
      assertThatThrownBy(() -> advice.beforeBodyWrite(
          originalResponse,
          methodParameter,
          mediaType,
          converterType,
          serverHttpRequest,
          serverHttpResponse
      ))
          .isInstanceOf(NoSuchMessageException.class)
          .hasMessageContaining(undefinedCode);

      verify(messageSource).getMessage(
          eq(undefinedCode),
          isNull(),
          eq(Locale.JAPAN)
      );
    }
  }
}
