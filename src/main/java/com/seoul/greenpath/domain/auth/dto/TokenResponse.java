package com.seoul.greenpath.domain.auth.dto;

/**
 * Access Token / Refresh Token 응답 DTO
 * - refreshToken은 HttpOnly Cookie로도 내려줄 수 있음 (보안 강화 시)
 * - 여기서는 Body 포함 방식으로 구현 (프론트와 협의하여 결정)
 */
public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long accessTokenExpiresIn  // 초(seconds) 단위
) {
    public static TokenResponse of(String accessToken, String refreshToken, long expiresIn) {
        return new TokenResponse(accessToken, refreshToken, "Bearer", expiresIn);
    }
}
