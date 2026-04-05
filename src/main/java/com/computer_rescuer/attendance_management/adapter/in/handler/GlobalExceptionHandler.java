package com.computer_rescuer.attendance_management.adapter.in.handler;

import com.computer_rescuer.attendance_management.adapter.in.model.ApiResponse;
import com.computer_rescuer.attendance_management.adapter.in.model.ResultCode;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Void>> handleValidationException(
      MethodArgumentNotValidException ex) {
    List<ApiResponse.Detail> details = ex.getBindingResult().getFieldErrors().stream()
        .map(error -> new ApiResponse.Detail(error.getField(), error.getDefaultMessage()))
        .toList();

    // ApiResponse.error(...) に Enum と詳細を渡すだけ。メッセージ解決は ApiResponseAdvice がやってくれる。
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiResponse.error(ResultCode.E_VALIDATION, details));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleSystemException(Exception ex) {
    log.error("システムエラー", ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiResponse.error(ResultCode.E_SYSTEM, null));
  }
}
