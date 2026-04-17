package com.computer_rescuer.attendance_management.infrastructure.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * HRMOS APIの設定値を保持するレコード（不変オブジェクト）
 */
@ConfigurationProperties(prefix = "app.hrmos.api")
public record HrmosProperties(
    String companyUrl,
    String secretKey,
    String baseUrl
) {

}
