package com.seoul.greenpath.global.exception;

import org.springframework.http.HttpStatus;

/**
 * 애플리케이션 전역 예외 - HTTP 상태코드를 함께 전달
 */
public class CustomException extends RuntimeException {

    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus() {
        return errorCode.getStatus();
    }

    public String getCode() {
        return errorCode.name();
    }
}
