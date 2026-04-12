package com.computer_rescuer.attendance_management.infrastructure.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * プロジェクト標準のJSONシリアライザ構成。
 * <p>
 * Spring Boot 4.xの自動構成に依存せず、明示的に定義することで コンテナ環境での起動確実性と、設定の透明性を担保します。
 * </p>
 */
@Configuration
public class JacksonConfig {

  @Bean
  @Primary // 他のライブラリが独自のMapperを持ち込んでも、こちらを優先させる
  public ObjectMapper objectMapper() {
    return new ObjectMapper()
        // Java 8 Date/Time APIのサポートを有効化
        .registerModule(new JavaTimeModule())
        // 日付を数値配列ではなくISO-8601文字列で出力
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        // JSONに未知の項目があってもエラーにせず、前方互換性を維持
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }
}
