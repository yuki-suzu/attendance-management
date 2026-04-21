package com.computer_rescuer.attendance_management.domain.model;

import java.util.Arrays;

/**
 * 打刻のアクション種別を表現する列挙型。
 * <p>
 * HRMOS API の仕様 (1:出勤, 2:退勤, 7:休憩開始, 8:休憩終了) に準拠しつつ、 システム内部で型安全に扱うためのドメイン型です。
 * </p>
 */
public enum StampType {

  CLOCK_IN(1, "出勤"),
  CLOCK_OUT(2, "退勤"),
  BREAK_START(7, "休憩開始"),
  BREAK_END(8, "休憩終了"),
  UNKNOWN(-1, "不明");

  private final int code;
  private final String description;

  StampType(int code, String description) {
    this.code = code;
    this.description = description;
  }

  public int getCode() {
    return code;
  }

  public String getDescription() {
    return description;
  }

  public static StampType fromCode(Integer code) {
    if (code == null) {
      return UNKNOWN;
    }
    return Arrays.stream(values())
        .filter(type -> type.code == code)
        .findFirst()
        .orElse(UNKNOWN);
  }
}
