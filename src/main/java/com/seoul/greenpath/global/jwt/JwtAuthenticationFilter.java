package com.seoul.greenpath.global.jwt;

import com.seoul.greenpath.global.exception.CustomException;
import com.seoul.greenpath.global.security.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 매 요청마다 JWT를 검증하는 Security Filter
 *
 * 처리 순서:
 * 1. Authorization 헤더에서 Bearer 토큰 추출
 * 2. 토큰 유효성 검사
 * 3. 회원 정보 조회 후 SecurityContext 등록
 *
 * ※ 토큰이 없거나 유효하지 않으면 인증 없이 통과 →
 *    이후 SecurityConfig의 인가 규칙에 따라 403/401 처리
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String token = resolveToken(request);

        if (StringUtils.hasText(token)) {
            try {
                if (jwtProvider.validateToken(token)) {
                    Long memberId = jwtProvider.getMemberId(token);
                    UserDetails userDetails = userDetailsService.loadUserById(memberId);

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (CustomException e) {
                log.debug("[JwtFilter] 토큰 검증 실패: {}", e.getMessage());
                // SecurityContext 초기화 없이 통과 → 이후 인가 필터에서 차단
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Authorization: Bearer {token} 헤더에서 토큰 추출
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
