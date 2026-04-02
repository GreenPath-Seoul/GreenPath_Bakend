package com.seoul.greenpath.global.jwt;

import com.seoul.greenpath.global.exception.CustomException;
import com.seoul.greenpath.global.exception.ErrorCode;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * JWT 토큰 생성 / 검증 / 파싱 전담 컴포넌트
 *
 * Access  Token : 짧은 수명 (기본 30분)  → Authorization 헤더로 전달
 * Refresh Token : 긴 수명   (기본 14일)  → DB + HttpOnly Cookie 또는 Body로 전달
 */
@Slf4j
@Component
public class JwtProvider {

    private final SecretKey key;
    private final long accessTokenValidityMs;
    private final long refreshTokenValidityMs;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-validity-seconds}") long accessTokenValiditySeconds,
            @Value("${jwt.refresh-token-validity-seconds}") long refreshTokenValiditySeconds
    ) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.accessTokenValidityMs  = accessTokenValiditySeconds  * 1000L;
        this.refreshTokenValidityMs = refreshTokenValiditySeconds * 1000L;
    }

    // ── 토큰 생성 ──────────────────────────────────────────────

    /**
     * Access Token 생성
     * @param memberId  회원 PK (subject)
     * @param role      권한 문자열 (ex. "ROLE_USER")
     */
    public String createAccessToken(Long memberId, String role) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + accessTokenValidityMs);

        return Jwts.builder()
                .subject(String.valueOf(memberId))
                .claim("role", role)
                .claim("type", "ACCESS")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    /**
     * Refresh Token 생성 (최소 정보만 포함 → DB가 진짜 저장소)
     */
    public String createRefreshToken(Long memberId) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + refreshTokenValidityMs);

        return Jwts.builder()
                .subject(String.valueOf(memberId))
                .claim("type", "REFRESH")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    // ── 토큰 파싱 / 검증 ──────────────────────────────────────

    /**
     * 토큰에서 Claims 추출 (서명 검증 포함)
     * 만료 / 서명 오류 시 CustomException 발생
     */
    public Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new CustomException(ErrorCode.EXPIRED_TOKEN);
        } catch (JwtException | IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
    }

    /**
     * 토큰 유효성 검사 (boolean 반환 — Filter 내부 사용)
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (CustomException e) {
            return false;
        }
    }

    /**
     * 토큰에서 회원 ID 추출
     */
    public Long getMemberId(String token) {
        return Long.parseLong(parseClaims(token).getSubject());
    }

    /**
     * 토큰에서 Role 추출
     */
    public String getRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    /**
     * Access Token 만료까지 남은 시간(ms) 반환
     * — 블랙리스트 TTL 계산 등에 활용
     */
    public long getExpiration(String token) {
        Date expiry = parseClaims(token).getExpiration();
        return expiry.getTime() - System.currentTimeMillis();
    }

    public long getAccessTokenValiditySeconds() {
        return accessTokenValidityMs / 1000;
    }

    public long getRefreshTokenValiditySeconds() {
        return refreshTokenValidityMs / 1000;
    }
}
