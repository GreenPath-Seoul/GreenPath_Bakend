package com.seoul.greenpath.global.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seoul.greenpath.global.exception.CustomException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * JWT 필터에서 발생하는 예외를 가로채서 JSON 응답으로 반환하는 필터
 */
@Slf4j
@Component
public class JwtExceptionFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } catch (CustomException ex) {
            setErrorResponse(response, ex.getStatus(), ex.getCode(), ex.getMessage());
        } catch (Exception ex) {
            log.error("필터 예기치 않은 오류 발생: ", ex);
            setErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다.");
        }
    }

    private void setErrorResponse(HttpServletResponse response, HttpStatus status, String code, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json;charset=UTF-8");

        ErrorResponse errorResponse = new ErrorResponse(LocalDateTime.now(), status.value(), code, message);
        response.getWriter().write(objectMapper.findAndRegisterModules().writeValueAsString(errorResponse));
    }

    private record ErrorResponse(LocalDateTime timestamp, int status, String code, String message) {}
}
