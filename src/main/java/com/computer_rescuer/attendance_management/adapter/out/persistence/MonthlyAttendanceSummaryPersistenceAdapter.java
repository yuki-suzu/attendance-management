package com.computer_rescuer.attendance_management.adapter.out.persistence;

import com.computer_rescuer.attendance_management.adapter.out.persistence.jooq.MonthlyAttendanceSummaryJooqRepository;
import com.computer_rescuer.attendance_management.application.port.out.MonthlyAttendanceSummaryRepositoryPort;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 月次勤怠サマリ出力ポート（{@link MonthlyAttendanceSummaryRepositoryPort}）の実装アダプター。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MonthlyAttendanceSummaryPersistenceAdapter implements
    MonthlyAttendanceSummaryRepositoryPort {

  private final MonthlyAttendanceSummaryJooqRepository jooqRepository;

  @Override
  public void upsertSummaries(String procMonth, String employeeNumber, List<SummaryItem> items) {
    jooqRepository.upsertSummaries(procMonth, employeeNumber, items);
  }

  @Override
  public List<String> findEmployeeNumbersWithUnsentSummary(String procMonth) {
    return jooqRepository.selectEmployeeNumbersWithUnsent(procMonth);
  }

  @Override
  public List<PersistedSummary> findSummariesByEmployeeNumbers(String procMonth,
      List<String> employeeNumbers) {
    return jooqRepository.selectSummariesByEmployeeNumbers(procMonth, employeeNumbers);
  }

  @Override
  public void markAsSent(String procMonth, List<String> employeeNumbers) {
    jooqRepository.updateMsgSendToSent(procMonth, employeeNumbers);
  }

  @Override
  public int deleteOlderThan(LocalDateTime thresholdDateTime) {
    return jooqRepository.deleteOlderThan(thresholdDateTime);
  }
}
