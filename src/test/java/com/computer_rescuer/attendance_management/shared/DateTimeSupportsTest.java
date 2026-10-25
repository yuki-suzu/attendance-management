package com.computer_rescuer.attendance_management.shared;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * {@link DateTimeSupports} の単体テストクラス。
 * <p>
 * システム共通の日時操作ユーティリティにおける日付加算、ISO 文字列変換、 null 安全性、およびプライベートコンストラクタの命令網羅（C0 100%）を検証します。
 * </p>
 */
@DisplayName("DateTimeSupports 単体テスト")
class DateTimeSupportsTest {

  @Nested
  @DisplayName("toEndOfDay メソッドの検証")
  class ToEndOfDayScenario {

    @Test
    @DisplayName("正常系: 指定日の翌日日付（plusDays(1)）が返却されること")
    void shouldReturnNextDay() {
      // [Given]
      LocalDate baseDate = LocalDate.of(2026, 4, 30);

      // [When]
      LocalDate actual = DateTimeSupports.toEndOfDay(baseDate);

      // [Then]
      assertEquals(LocalDate.of(2026, 5, 1), actual, "月またぎを含めて翌日日付が算出されること");
    }

    @Test
    @DisplayName("null 安全性: 引数が null の場合、null を返却すること")
    void shouldReturnNullWhenDateIsNull() {
      assertNull(DateTimeSupports.toEndOfDay(null));
    }
  }

  @Nested
  @DisplayName("localDateToIsoDateTimeString メソッドの検証")
  class LocalDateToIsoDateTimeStringScenario {

    @Test
    @DisplayName("正常系: LocalDate が JST 00:00:00 の ISO-8601 オフセット文字列へ変換されること")
    void shouldConvertToIsoStringInJst() {
      // [Given]
      LocalDate date = LocalDate.of(2026, 4, 1);

      // [When]
      String actual = DateTimeSupports.localDateToIsoDateTimeString(date);

      // [Then]
      assertEquals("2026-04-01T00:00:00+09:00", actual,
          "JST オフセット付きの開始時刻文字列になること");
    }

    @Test
    @DisplayName("null 安全性: 引数が null の場合、null を返却すること")
    void shouldReturnNullWhenDateIsNull() {
      assertNull(DateTimeSupports.localDateToIsoDateTimeString(null));
    }
  }

  @Nested
  @DisplayName("プライベートコンストラクタの構造検証")
  class ConstructorScenario {

    @Test
    @DisplayName("インスタンス化抑止: コンストラクタが private であり、リフレクションで呼び出し可能であること（C0 網羅）")
    void shouldVerifyPrivateConstructor() throws NoSuchMethodException {
      // [Given]
      Constructor<DateTimeSupports> constructor = DateTimeSupports.class.getDeclaredConstructor();

      // [When]
      boolean isPrivate = Modifier.isPrivate(constructor.getModifiers());
      constructor.setAccessible(true);

      // [Then]
      assertAll(
          () -> assertTrue(isPrivate, "コンストラクタが private であること"),
          () -> {
            try {
              DateTimeSupports instance = constructor.newInstance();
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
