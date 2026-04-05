package com.computer_rescuer.attendance_management.adapter.out.hrmos.model;

/**
 * HRMOSの認証Token取得APIのレスポンス
 */
public record HrmosTokenResponse(
    String token,
    String expiredAt
) {

}
