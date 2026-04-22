package com.computer_rescuer.attendance_management.infrastructure.config;

import com.computer_rescuer.attendance_management.adapter.in.handler.ScheduledTaskErrorHandler;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * スケジューラーおよび分散ロック（ShedLock）の設定クラス。
 */
@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT10M")
@RequiredArgsConstructor
public class SchedulerConfig {

  private final ScheduledTaskErrorHandler scheduledTaskErrorHandler;

  /**
   * ShedLock が PostgreSQL を使ってロック管理を行うためのプロバイダー設定
   */
  @Bean
  public LockProvider lockProvider(DataSource dataSource) {
    return new JdbcTemplateLockProvider(
        JdbcTemplateLockProvider.Configuration.builder()
            .withJdbcTemplate(new JdbcTemplate(dataSource))
            .usingDbTime() // DB側の時計を使う（サーバー間の時計ズレ対策）
            .build()
    );
  }

  @Bean
  public ThreadPoolTaskScheduler taskScheduler() {
    ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    scheduler.setPoolSize(5);
    scheduler.setThreadNamePrefix("batch-thread-");
    scheduler.setWaitForTasksToCompleteOnShutdown(true);
    scheduler.setAwaitTerminationSeconds(60);
    scheduler.setErrorHandler(scheduledTaskErrorHandler);
    return scheduler;
  }
}
