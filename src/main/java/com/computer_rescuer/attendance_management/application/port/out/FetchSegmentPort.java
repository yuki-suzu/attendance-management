package com.computer_rescuer.attendance_management.application.port.out;

import com.computer_rescuer.attendance_management.domain.model.Segment;
import java.util.List;

/**
 * 保存されている勤務区分情報を取得する出力ポート。
 */
public interface FetchSegmentPort {

  /**
   * すべての勤務区分を取得します。
   */
  List<Segment> fetchAll();
}
