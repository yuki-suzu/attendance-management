package com.computer_rescuer.attendance_management.adapter.in.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * REST APIの共通レスポンスDTO（エンベロープ）。 成功時・エラー時を問わず、一貫したJSON構造をクライアントに提供します。
 *
 * @param <T> 動的に追加されるデータの型（GET等の結果）
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY) // nullや空リストの場合はJSONに出力しない
public record ApiResponse<T>(
    String code,
    String message,
    List<Detail> detail,
    T data
) {

  /**
   * エラー詳細やバリデーション情報を格納するレコード
   */
  public record Detail(
      String key,
      String message
  ) {

  }

  // =========================================================================
  // 静的ファクトリメソッド群
  // =========================================================================

  /**
   * [成功] 返却データなし（POST/PUT/DELETEなどでメッセージのみ返す場合）
   */
  public static ApiResponse<Void> success() {
    return new ApiResponse<>(ResultCode.SUCCESS.name(), null, null, null);
  }

  /**
   * [成功] 返却データあり（GETなどでデータを返す場合）
   */
  public static <T> ApiResponse<T> success(T data) {
    return new ApiResponse<>(ResultCode.SUCCESS.name(), null, null, data);
  }

  /**
   * [エラー] エラーメッセージと詳細情報（バリデーションエラー等）を返す場合
   */
  public static ApiResponse<Void> error(ResultCode resultCode, List<Detail> detail) {
    return new ApiResponse<>(resultCode.name(), null, detail, null);
  }
}
