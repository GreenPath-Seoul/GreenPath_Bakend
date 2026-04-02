package com.seoul.greenpath.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Access Token 재발급 요청 DTO
 */
public record ReissueRequest(

        @NotBlank(message = "리프레시 토큰은 필수입니다.")
        String refreshToken
) {}
