package com.seoul.greenpath.global.oauth2;

import com.seoul.greenpath.domain.auth.entity.RefreshToken;
import com.seoul.greenpath.domain.auth.repository.RefreshTokenRepository;
import com.seoul.greenpath.global.jwt.JwtProvider;
import com.seoul.greenpath.global.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * OAuth2 로그인 성공 핸들러
 *
 * 처리 순서:
 * 1. CustomUserDetails에서 memberId, role 추출
 * 2. Access Token + Refresh Token 발급
 * 3. Refresh Token DB 저장 (Token Rotation 전략)
 * 4. 프론트엔드로 토큰을 쿼리 파라미터에 실어 리다이렉트
 *    (프로덕션에서는 HttpOnly Cookie 권장)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long memberId = userDetails.getMemberId();
        String role   = userDetails.getAuthorities().iterator().next().getAuthority();

        // 토큰 발급
        String accessToken  = jwtProvider.createAccessToken(memberId, role);
        String refreshToken = jwtProvider.createRefreshToken(memberId);

        // Refresh Token DB 저장 (기존 토큰 교체)
        refreshTokenRepository.findByMemberId(memberId)
                .ifPresentOrElse(
                        existing -> existing.rotate(
                                refreshToken,
                                LocalDateTime.now().plusSeconds(jwtProvider.getRefreshTokenValiditySeconds()),
                                getClientIp(request)
                        ),
                        () -> refreshTokenRepository.save(
                                RefreshToken.builder()
                                        .memberId(memberId)
                                        .token(refreshToken)
                                        .expiresAt(LocalDateTime.now()
                                                .plusSeconds(jwtProvider.getRefreshTokenValiditySeconds()))
                                        .lastUsedIp(getClientIp(request))
                                        .build()
                        )
                );

        log.info("[OAuth2 Success] memberId={}", memberId);

        // 프론트엔드 콜백 URL로 리다이렉트
        String targetUrl = UriComponentsBuilder
                .fromUriString(frontendUrl + "/oauth2/callback")
                .queryParam("accessToken",  accessToken)
                .queryParam("refreshToken", refreshToken)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        return (ip != null && !ip.isEmpty()) ? ip.split(",")[0].trim() : request.getRemoteAddr();
    }
}
