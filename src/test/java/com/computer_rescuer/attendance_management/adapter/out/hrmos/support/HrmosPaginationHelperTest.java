package com.computer_rescuer.attendance_management.adapter.out.hrmos.support;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;
import java.util.function.IntFunction;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link HrmosPaginationHelper} の単体テストクラス。
 * <p>
 * 外部 API のページネーション処理における初回空データ終了、1ページ完結、 複数ページにまたがる全件取得結合、ちょうど100件の境界値判定、 および Lombok の
 * {@code @NonNull} バリデーションの命令網羅（C0 100%）を検証します。 Spring コンテキストを起動せず、純粋な Mockito 単体テストとして高速に実行します。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HrmosPaginationHelper 単体テスト")
class HrmosPaginationHelperTest {

  @Mock
  private IntFunction<List<String>> apiCall;

  private final String logLabel = "テストデータ";

  @Nested
  @DisplayName("引数バリデーション（@NonNull）シナリオ")
  class ValidationScenario {

    /**
     * apiCall 引数に null が渡された場合、Lombok の @NonNull 機構により 即座に NullPointerException がスローされることを検証します。
     */
    @Test
    @DisplayName("apiCall が null の場合、NullPointerException をスローすること")
    void shouldThrowNullPointerExceptionWhenApiCallIsNull() {
      // [When & Then]
      NullPointerException exception = assertThrows(
          NullPointerException.class,
          () -> HrmosPaginationHelper.fetchAllPages(logLabel, null),
          "apiCall に null を渡した場合は早期に NullPointerException がスローされること"
      );

      assertNotNull(exception.getMessage(), "例外メッセージが存在すること");
    }
  }

  @Nested
  @DisplayName("データ件数に応じたページネーション終了シナリオ")
  class PaginationExecutionScenario {

    /**
     * 初回呼び出し（page: 1）で null が返却された場合、ループを即時終了して 空リストが返却されることを検証します（CollectionUtils.isEmpty の null
     * 分岐網羅）。
     */
    @Test
    @DisplayName("初回取得結果が null の場合、ループを即時終了し空リストを返却すること")
    void shouldReturnEmptyListWhenFirstPageReturnsNull() {
      // [Given]
      given(apiCall.apply(1)).willReturn(null);

      // [When]
      List<String> actual = HrmosPaginationHelper.fetchAllPages(logLabel, apiCall);

      // [Then]
      assertAll(
          () -> assertNotNull(actual, "返却リストが null でないこと"),
          () -> assertTrue(actual.isEmpty(), "空リストが返却されること")
      );
      then(apiCall).should(times(1)).apply(1);
    }

    /**
     * 初回呼び出し（page: 1）で空リストが返却された場合、ループを即時終了して 空リストが返却されることを検証します（CollectionUtils.isEmpty の empty
     * 分岐網羅）。
     */
    @Test
    @DisplayName("初回取得結果が空リストの場合、ループを即時終了し空リストを返却すること")
    void shouldReturnEmptyListWhenFirstPageReturnsEmpty() {
      // [Given]
      given(apiCall.apply(1)).willReturn(Collections.emptyList());

      // [When]
      List<String> actual = HrmosPaginationHelper.fetchAllPages(logLabel, apiCall);

      // [Then]
      assertAll(
          () -> assertNotNull(actual, "返却リストが null でないこと"),
          () -> assertTrue(actual.isEmpty(), "空リストが返却されること")
      );
      then(apiCall).should(times(1)).apply(1);
    }

    /**
     * 初回呼び出しで取得件数が 100 件未満（例: 5件）の場合、 2 ページ目をリクエストせずに 1 ページのみで処理を終了することを検証します。
     */
    @Test
    @DisplayName("1ページ目の取得件数が100件未満の場合、次ページをリクエストせず1回で終了すること")
    void shouldCompleteInSinglePageWhenCountIsLessThanLimit() {
      // [Given]
      List<String> page1Data = List.of("item1", "item2", "item3", "item4", "item5");
      given(apiCall.apply(1)).willReturn(page1Data);

      // [When]
      List<String> actual = HrmosPaginationHelper.fetchAllPages(logLabel, apiCall);

      // [Then]
      assertAll(
          () -> assertEquals(5, actual.size(), "件数が一致すること"),
          () -> assertEquals(page1Data, actual, "取得したリストの内容が一致すること")
      );
      then(apiCall).should(times(1)).apply(1);
    }

    /**
     * 1 ページ目に上限値（100件）取得でき、2 ページ目で 100 件未満（50件）が返却された場合、 両ページのデータが結合されて全 150 件が返却されることを検証します。
     */
    @Test
    @DisplayName("複数ページにまたがる場合、全ページのデータを結合して返却すること")
    void shouldFetchAndCombineMultiplePages() {
      // [Given]
      List<String> page1Data = IntStream.rangeClosed(1, 100)
          .mapToObj(i -> "Page1-Item" + i)
          .toList();
      List<String> page2Data = IntStream.rangeClosed(1, 50)
          .mapToObj(i -> "Page2-Item" + i)
          .toList();

      given(apiCall.apply(1)).willReturn(page1Data);
      given(apiCall.apply(2)).willReturn(page2Data);

      // [When]
      List<String> actual = HrmosPaginationHelper.fetchAllPages(logLabel, apiCall);

      // [Then]
      assertAll(
          () -> assertEquals(150, actual.size(), "合計件数が 150 件であること"),
          () -> assertEquals("Page1-Item1", actual.getFirst(), "先頭要素が一致すること"),
          () -> assertEquals("Page2-Item50", actual.getLast(), "末尾要素が一致すること")
      );
      then(apiCall).should().apply(1);
      then(apiCall).should().apply(2);
    }

    /**
     * 総件数がちょうど 100 件（1ページ目: 100件、2ページ目: 空リスト）の境界値において、 2 ページ目の空判定で安全にループを脱出し、全 100 件を返却することを検証します。
     */
    @Test
    @DisplayName("ちょうど100件取得時、次ページが空リストであることを検知して正常終了すること")
    void shouldHandleBoundaryConditionWhenTotalCountIsExactlyLimit() {
      // [Given]
      List<String> page1Data = IntStream.rangeClosed(1, 100)
          .mapToObj(i -> "Item" + i)
          .toList();

      given(apiCall.apply(1)).willReturn(page1Data);
      given(apiCall.apply(2)).willReturn(Collections.emptyList());

      // [When]
      List<String> actual = HrmosPaginationHelper.fetchAllPages(logLabel, apiCall);

      // [Then]
      assertAll(
          () -> assertEquals(100, actual.size(), "合計件数が 100 件であること"),
          () -> assertEquals("Item100", actual.getLast(), "全件取得されていること")
      );
      then(apiCall).should().apply(1);
      then(apiCall).should().apply(2);
    }
  }

  @Nested
  @DisplayName("ユーティリティクラス構造シナリオ")
  class UtilityClassStructureScenario {

    /**
     * ユーティリティクラスのプライベートコンストラクタをリフレクション経由で呼び出し、 インスタンス化の隠蔽性確認と同時に JaCoCo カバレッジの 100% 達成を検証します。
     */
    @Test
    @DisplayName("プライベートコンストラクタが定義されており、インスタンス化可能であること（C0 網羅）")
    void shouldInvokePrivateConstructorForCoverage() throws NoSuchMethodException {
      // [Given]
      Constructor<HrmosPaginationHelper> constructor =
          HrmosPaginationHelper.class.getDeclaredConstructor();

      // [When]
      boolean isPrivate = Modifier.isPrivate(constructor.getModifiers());
      constructor.setAccessible(true);

      // [Then]
      assertAll(
          () -> assertTrue(isPrivate, "コンストラクタが private であること"),
          () -> {
            try {
              HrmosPaginationHelper instance = constructor.newInstance();
              assertNotNull(instance, "インスタンスが生成できること");
            } catch (InvocationTargetException | InstantiationException |
                     IllegalAccessException e) {
              throw new AssertionError("コンストラクタの呼び出しに失敗しました", e);
            }
          }
      );
    }
  }
}
