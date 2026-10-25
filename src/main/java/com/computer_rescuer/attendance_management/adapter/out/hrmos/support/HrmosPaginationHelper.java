package com.computer_rescuer.attendance_management.adapter.out.hrmos.support;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

/**
 * HRMOS API のページネーション処理を共通化する汎用ヘルパークラス。
 * <p>
 * 外部 API からの複数ページにまたがるデータ取得をカプセル化し、全件を取得・結合したリストを構築します。
 * </p>
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HrmosPaginationHelper {

  /**
   * HRMOS API の1ページあたりの最大取得件数。
   */
  private static final int API_FETCH_LIMIT = 100;

  /**
   * ページネーションをループ処理し、全件を取得して結合したリストを返します。
   *
   * @param <T>      取得する生データの型（HrmosStampLog, HrmosUser など）
   * @param logLabel ログ出力用のデータ名（例: "日次打刻ログ", "従業員情報"）
   * @param apiCall  ページ番号（1から開始）を受け取り、そのページの API 結果リストを返す関数
   * @return 全ページ分を結合した生データのリスト
   * @throws NullPointerException {@code apiCall} が null の場合
   */
  public static <T> List<T> fetchAllPages(String logLabel, @NonNull IntFunction<List<T>> apiCall) {
    List<T> allData = new ArrayList<>();
    int page = 1;

    while (true) {
      List<T> paged = apiCall.apply(page);

      if (CollectionUtils.isEmpty(paged)) {
        break;
      }

      allData.addAll(paged);
      log.debug("🔍 HRMOSから {} を {} 件取得しました (page: {})", logLabel, paged.size(), page);

      if (paged.size() < API_FETCH_LIMIT) {
        break;
      }
      page++;
    }

    return allData;
  }
}
