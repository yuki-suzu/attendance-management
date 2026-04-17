package com.computer_rescuer.attendance_management.adapter.out.hrmos.client;

import com.computer_rescuer.attendance_management.adapter.out.hrmos.model.HrmosDailyWorkOutput;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HrmosWorkOutputApi {

  private final HrmosCoreHttpClient coreClient;

  public List<HrmosDailyWorkOutput> fetchDailyWorkOutputs(String token, String date, int page) {
    String path = String.format("/work_outputs/daily/%s", date);
    return coreClient.fetchAndParseList(
        token, path, page, "work_outputs", "日次勤怠", new TypeReference<>() {
        }
    );
  }
}
