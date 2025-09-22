package com.dgu.review.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Slf4j
public class ApiResponse<T> {

    private String errorCode;
    private String message = "OK";
    private T result;

    // 성공 응답
    public static <T> ApiResponse<T> ok(T result) {
        return ApiResponse.<T>builder()
                .errorCode(null)
                .message("OK")
                .result(result)
                .build();
    }

    // 실패 응답 
    public static <T> ApiResponse<T> error(String errorCode) {
        return ApiResponse.<T>builder()
                .errorCode(errorCode)
                .message("error")
                .result(null)
                .build();
    }
}
