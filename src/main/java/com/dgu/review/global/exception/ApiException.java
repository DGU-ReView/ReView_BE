package com.dgu.review.global.exception;

// 서비스 에러
public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;

    // ErrorCode의 기본 메시지 사용
    public ApiException(ErrorCode errorCode) {
        super(errorCode.defaultMessage());
        this.errorCode = errorCode;
    }

    // 커스텀 메시지로 덮어쓰기
    public ApiException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() { return errorCode; }
}
