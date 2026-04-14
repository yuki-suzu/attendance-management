package com.computer_rescuer.attendance_management.adapter.out.hrmos;

import com.computer_rescuer.attendance_management.adapter.out.hrmos.client.HrmosAuthApi;
import com.computer_rescuer.attendance_management.adapter.out.hrmos.client.HrmosWorkOutputApi;
import com.computer_rescuer.attendance_management.adapter.out.hrmos.mapper.HrmosWorkOutputMapper;
import com.computer_rescuer.attendance_management.adapter.out.hrmos.model.HrmosDailyWorkOutput;
import com.computer_rescuer.attendance_management.application.port.out.FetchDailyWorkRecordPort;
import com.computer_rescuer.attendance_management.domain.model.DailyWorkRecord;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * HRMOSから日次勤怠実績を取得するアダプター。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HrmosDailyWorkRecordAdapter implements FetchDailyWorkRecordPort {

  private final HrmosAuthApi authApi;
  private final HrmosWorkOutputApi workOutputApi;
  private final HrmosWorkOutputMapper mapper;

  @Override
  public List<DailyWorkRecord> fetchByDate(LocalDate date) {
    String token = authApi.fetchToken();
    List<HrmosDailyWorkOutput> allRawData = new ArrayList<>();

    int page = 1;
    while (true) {
      List<HrmosDailyWorkOutput> paged = workOutputApi.fetchDailyWorkOutputs(token, date.toString(),
          page);
      if (paged == null || paged.isEmpty()) {
        break;
      }
      allRawData.addAll(paged);
      if (paged.size() < 100) {
        break;
      }
      page++;
    }

    return allRawData.stream().map(mapper::toDomain).toList();
  }
}
