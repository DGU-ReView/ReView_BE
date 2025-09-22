package com.dgu.review.global.exception;

//서비스 에러 
public class ApiException extends RuntimeException {
 private final String errorCode;
 private final int httpStatus;

 public ApiException(String errorCode, int httpStatus) {
     super(errorCode);
     this.errorCode = errorCode;
     this.httpStatus = httpStatus;
 }


 public String getErrorCode() { return errorCode; }
 public int getHttpStatus() { return httpStatus; }
}
