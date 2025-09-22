package com.dgu.review.global.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.dgu.review.global.response.ApiResponse;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

 @ExceptionHandler(ApiException.class)
 public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException ex) {
     ApiResponse<Void> body = ApiResponse.error(ex.getErrorCode());
     
     
     return ResponseEntity.status(ex.getHttpStatus()).body(body);
 }
 

 // 바인딩 실패 
 @ExceptionHandler({ MethodArgumentNotValidException.class, BindException.class, ConstraintViolationException.class })
 public ResponseEntity<ApiResponse<Void>> handleValidation(Exception ex) {
     ApiResponse<Void> body = ApiResponse.error("VALIDATION_ERROR");
     return ResponseEntity.status(422).body(body);
 }
 
 // 그 외 예상 못한 예외 → 500
 @ExceptionHandler(Exception.class)
 public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
     // 로그는 여기서 남기고, 응답은 규격 유지
	 log.error("Unexpected error", ex);
     ApiResponse<Void> body = ApiResponse.error("INTERNAL_ERROR");
     return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
 }
}

