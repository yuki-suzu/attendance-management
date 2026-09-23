package com.computer_rescuer.attendance_management.adapter.out.persistence.jooq;

import static com.computer_rescuer.attendance_management.generated.jooq.Tables.T_MONTHLY_ATTENDANCE_SUMMARY;

import com.computer_rescuer.attendance_management.application.port.out.MonthlyAttendanceSummaryRepositoryPort.PersistedSummary;
import com.computer_rescuer.attendance_management.application.port.out.MonthlyAttendanceSummaryRepositoryPort.SummaryItem;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

/**
 * 月次勤怠サマリテーブルに対する jOOQ リポジトリ。
 */
@Repository
@RequiredArgsConstructor
public class MonthlyAttendanceSummaryJooqRepository {

  private final DSLContext dsl;

  /**
   * 複合主キーで UPSERT を行い、件数に変動があった場合のみ msg_send を '0' に倒します。
   */
  public void upsertSummaries(String procMonth, String employeeNumber, List<SummaryItem> items) {
    if (items == null || items.isEmpty()) {
      return;
    }

    var queries = items.stream()
        .map(item -> dsl.insertInto(T_MONTHLY_ATTENDANCE_SUMMARY)
            .set(T_MONTHLY_ATTENDANCE_SUMMARY.PROC_MONTH, procMonth)
            .set(T_MONTHLY_ATTENDANCE_SUMMARY.EMPLOYEE_NUMBER, employeeNumber)
            .set(T_MONTHLY_ATTENDANCE_SUMMARY.SEGMENT_ID, item.segmentId())
            .set(T_MONTHLY_ATTENDANCE_SUMMARY.SEGMENT_TITLE, item.segmentTitle())
            .set(T_MONTHLY_ATTENDANCE_SUMMARY.COUNT, item.count())
            .set(T_MONTHLY_ATTENDANCE_SUMMARY.MSG_SEND, item.msgSend())
            .onConflict(
                T_MONTHLY_ATTENDANCE_SUMMARY.PROC_MONTH,
                T_MONTHLY_ATTENDANCE_SUMMARY.EMPLOYEE_NUMBER,
                T_MONTHLY_ATTENDANCE_SUMMARY.SEGMENT_ID,
                T_MONTHLY_ATTENDANCE_SUMMARY.SEGMENT_TITLE
            )
            .doUpdate()
            .set(T_MONTHLY_ATTENDANCE_SUMMARY.MSG_SEND,
                DSL.case_()
                    .when(T_MONTHLY_ATTENDANCE_SUMMARY.SEGMENT_ID.eq(2), "1")
                    .when(T_MONTHLY_ATTENDANCE_SUMMARY.COUNT.ne(
                        DSL.excluded(T_MONTHLY_ATTENDANCE_SUMMARY.COUNT)), "0")
                    .otherwise(T_MONTHLY_ATTENDANCE_SUMMARY.MSG_SEND))
            .set(T_MONTHLY_ATTENDANCE_SUMMARY.COUNT,
                DSL.excluded(T_MONTHLY_ATTENDANCE_SUMMARY.COUNT))
            .set(T_MONTHLY_ATTENDANCE_SUMMARY.UPDATED_AT, OffsetDateTime.now()))
        .toList();

    dsl.batch(queries).execute();
  }

  /**
   * msg_send = '0' を持つ従業員番号を取得します。
   */
  public List<String> selectEmployeeNumbersWithUnsent(String procMonth) {
    return dsl.selectDistinct(T_MONTHLY_ATTENDANCE_SUMMARY.EMPLOYEE_NUMBER)
        .from(T_MONTHLY_ATTENDANCE_SUMMARY)
        .where(T_MONTHLY_ATTENDANCE_SUMMARY.PROC_MONTH.eq(procMonth))
        .and(T_MONTHLY_ATTENDANCE_SUMMARY.MSG_SEND.eq("0"))
        .fetch(T_MONTHLY_ATTENDANCE_SUMMARY.EMPLOYEE_NUMBER);
  }

  /**
   * 指定従業員群の当月レコードを全量取得します。
   */
  public List<PersistedSummary> selectSummariesByEmployeeNumbers(String procMonth,
      List<String> employeeNumbers) {
    if (employeeNumbers == null || employeeNumbers.isEmpty()) {
      return List.of();
    }

    return dsl.selectFrom(T_MONTHLY_ATTENDANCE_SUMMARY)
        .where(T_MONTHLY_ATTENDANCE_SUMMARY.PROC_MONTH.eq(procMonth))
        .and(T_MONTHLY_ATTENDANCE_SUMMARY.EMPLOYEE_NUMBER.in(employeeNumbers))
        .orderBy(T_MONTHLY_ATTENDANCE_SUMMARY.EMPLOYEE_NUMBER,
            T_MONTHLY_ATTENDANCE_SUMMARY.SEGMENT_ID)
        .fetch(r -> new PersistedSummary(
            r.getProcMonth(),
            r.getEmployeeNumber(),
            r.getSegmentId(),
            r.getSegmentTitle(),
            r.getCount(),
            r.getMsgSend()
        ));
  }

  /**
   * 送信完了フラグを '1' に更新します。
   */
  public void updateMsgSendToSent(String procMonth, List<String> employeeNumbers) {
    if (employeeNumbers == null || employeeNumbers.isEmpty()) {
      return;
    }

    dsl.update(T_MONTHLY_ATTENDANCE_SUMMARY)
        .set(T_MONTHLY_ATTENDANCE_SUMMARY.MSG_SEND, "1")
        .set(T_MONTHLY_ATTENDANCE_SUMMARY.UPDATED_AT, OffsetDateTime.now())
        .where(T_MONTHLY_ATTENDANCE_SUMMARY.PROC_MONTH.eq(procMonth))
        .and(T_MONTHLY_ATTENDANCE_SUMMARY.EMPLOYEE_NUMBER.in(employeeNumbers))
        .execute();
  }

  /**
   * 最終更新日が閾値より前のレコードを物理削除します。
   */
  public int deleteOlderThan(LocalDateTime thresholdDateTime) {
    return dsl.deleteFrom(T_MONTHLY_ATTENDANCE_SUMMARY)
        .where(T_MONTHLY_ATTENDANCE_SUMMARY.UPDATED_AT.lt(
            thresholdDateTime.atOffset(OffsetDateTime.now().getOffset())))
        .execute();
  }
}
