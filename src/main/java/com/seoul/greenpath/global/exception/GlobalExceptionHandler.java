package com.seoul.greenpath.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * @Valid 검증 실패
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "유효하지 않은 값",
                        (existing, replacement) -> existing
                ));

        ErrorResponse response = ErrorResponse.of(
                ErrorCode.INVALID_INPUT_VALUE.getStatus().value(),
                "INVALID_INPUT_VALUE",
                "입력값 검증에 실패했습니다.",
                fieldErrors
        );
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 커스텀 비즈니스 예외
     */
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException ex) {
        log.warn("[CustomException] code={}, message={}", ex.getCode(), ex.getMessage());
        ErrorResponse response = ErrorResponse.of(
                ex.getStatus().value(),
                ex.getCode(),
                ex.getMessage()
        );
        return ResponseEntity.status(ex.getStatus()).body(response);
    }

    /**
     * Spring Security 인증 실패
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex) {
        log.warn("[AuthenticationException] {}", ex.getMessage());
        ErrorResponse response = ErrorResponse.of(401, "UNAUTHORIZED", ex.getMessage());
        return ResponseEntity.status(401).body(response);
    }

    /**
     * Spring Security 권한 없음
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("[AccessDeniedException] {}", ex.getMessage());
        ErrorResponse response = ErrorResponse.of(403, "FORBIDDEN", "접근 권한이 없습니다.");
        return ResponseEntity.status(403).body(response);
    }

    /**
     * 예상치 못한 서버 오류
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        log.error("[UnhandledException]", ex);
        ErrorResponse response = ErrorResponse.of(500, "INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다.");
        return ResponseEntity.internalServerError().body(response);
    }

    // ── 내부 응답 포맷 ──────────────────────────────────────
    public record ErrorResponse(
            LocalDateTime timestamp,
            int status,
            String code,
            String message,
            Object errors
    ) {
        public static ErrorResponse of(int status, String code, String message) {
            return new ErrorResponse(LocalDateTime.now(), status, code, message, null);
        }

        public static ErrorResponse of(int status, String code, String message, Object errors) {
            return new ErrorResponse(LocalDateTime.now(), status, code, message, errors);
        }
    }
}
