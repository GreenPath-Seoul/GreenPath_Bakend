package com.seoul.greenpath.global.common;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * API 공통 응답 포맷
 */
@Getter
public class ApiResponse<T> {

    private final LocalDateTime timestamp;
    private final int status;
    private final String message;
    private final T data;

    private ApiResponse(int status, String message, T data) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "요청이 성공적으로 처리되었습니다.", data);
    }

    public static <T> ApiResponse<T> success(int status, String message, T data) {
        return new ApiResponse<>(status, message, data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(200, message, data);
    }
}
