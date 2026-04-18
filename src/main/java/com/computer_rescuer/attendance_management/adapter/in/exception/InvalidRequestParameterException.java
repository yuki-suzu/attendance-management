package com.computer_rescuer.attendance_management.adapter.in.exception;

/**
 * クライアントからのリクエストパラメータがビジネスルールや相関バリデーションに違反している場合にスローされる例外。
 * <p>
 * この例外は、HTTP 400 (Bad Request) としてクライアントに返却されるべき 「意図的な入力エラー」を示すために使用します。
 * </p>
 */
public class InvalidRequestParameterException extends RuntimeException {

  public InvalidRequestParameterException(String message) {
    super(message);
  }
}
