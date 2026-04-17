package com.computer_rescuer.attendance_management.infrastructure.config;

import org.jooq.conf.RenderNameCase;
import org.jooq.conf.RenderQuotedNames;
import org.springframework.boot.jooq.autoconfigure.DefaultConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * jOOQの実行時設定をカスタマイズするコンフィギュレーションクラス。
 * <p>
 * PostgreSQLとjOOQの間で発生する、識別子（テーブル名・カラム名）の大文字・小文字に関する インピーダンスミスマッチを解消するための設定を行います。
 * </p>
 */
@Configuration
public class JooqConfig {

  /**
   * jOOQがSQLを構築する際のレンダリング（文字列化）ルールをカスタマイズします。
   * <p>
   * 以下の設定により、jOOQが生成するSQLは {@code delete from "M_EMPLOYEE"} ではなく {@code delete from m_employee}
   * となり、PostgreSQLで正常に実行可能となります。
   * </p>
   *
   * @return Spring BootのjOOQ自動構成に適用されるカスタマイザー
   */
  @Bean
  public DefaultConfigurationCustomizer jooqConfigurationCustomizer() {
    return configuration -> configuration.settings()
        // 1. テーブル名やカラム名をダブルクォーテーション(")で囲まないように設定
        .withRenderQuotedNames(RenderQuotedNames.NEVER)
        // 2. 念のため、SQL生成時に識別子をすべて小文字として出力するように強制
        .withRenderNameCase(RenderNameCase.LOWER);
  }
}
