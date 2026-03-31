package com.seoul.greenpath.domain.auth.controller;

import com.seoul.greenpath.domain.auth.dto.LoginRequest;
import com.seoul.greenpath.domain.auth.dto.ReissueRequest;
import com.seoul.greenpath.domain.auth.dto.SignUpRequest;
import com.seoul.greenpath.domain.auth.dto.TokenResponse;
import com.seoul.greenpath.domain.auth.service.AuthService;
import com.seoul.greenpath.global.common.ApiResponse;
import com.seoul.greenpath.global.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 인증 REST API 컨트롤러
 *
 * POST /api/auth/sign-up    — 일반 회원가입
 * POST /api/auth/login      — 일반 로그인
 * POST /api/auth/reissue    — Access Token 재발급
 * POST /api/auth/logout     — 로그아웃
 *
 * OAuth2 로그인 진입점:
 * GET  /oauth2/authorization/kakao  — 카카오 로그인 시작 (Spring Security 자동 처리)
 * GET  /oauth2/authorization/naver  — 네이버 로그인 시작 (Spring Security 자동 처리)
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 이메일 중복 확인
     */
    @GetMapping("/check-email")
    public ResponseEntity<ApiResponse<Void>> checkEmail(@RequestParam String email) {
        authService.checkEmailDuplication(email);
        return ResponseEntity.ok(ApiResponse.success("사용 가능한 이메일입니다.", null));
    }

    /**
     * 일반 회원가입
     */
    @PostMapping("/sign-up")
    public ResponseEntity<ApiResponse<Long>> signUp(@Valid @RequestBody SignUpRequest request) {
        Long memberId = authService.signUp(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "회원가입이 완료되었습니다.", memberId));
    }

    /**
     * 일반 로그인
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse tokenResponse = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("로그인 성공", tokenResponse));
    }

    /**
     * Access Token 재발급 (Refresh Token Rotation)
     */
    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<TokenResponse>> reissue(
            @Valid @RequestBody ReissueRequest request,
            HttpServletRequest servletRequest
    ) {
        String clientIp = resolveClientIp(servletRequest);
        TokenResponse tokenResponse = authService.reissue(request, clientIp);
        return ResponseEntity.ok(ApiResponse.success("토큰 재발급 성공", tokenResponse));
    }

    /**
     * 로그아웃 (Refresh Token DB 삭제)
     * 인증된 사용자만 호출 가능
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        authService.logout(userDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.success("로그아웃 되었습니다.", null));
    }

    // ── 내부 유틸 ─────────────────────────────────────────────

    private String resolveClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        return (ip != null && !ip.isEmpty()) ? ip.split(",")[0].trim() : request.getRemoteAddr();
    }
}
