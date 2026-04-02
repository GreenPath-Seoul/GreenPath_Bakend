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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "인증(로그인, 회원가입, 토큰) 관련 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 이메일 중복 확인
     */
    @Operation(summary = "이메일 중복 확인", description = "회원가입 전 이메일 중복 여부를 확인합니다.")
    @GetMapping("/check-email")
    public ResponseEntity<ApiResponse<Void>> checkEmail(@RequestParam String email) {
        authService.checkEmailDuplication(email);
        return ResponseEntity.ok(ApiResponse.success("사용 가능한 이메일입니다.", null));
    }

    /**
     * 일반 회원가입
     */
    @Operation(summary = "회원 가입", description = "기본 정보(이메일, 비밀번호, 이름)로 회원가입을 진행합니다.")
    @PostMapping("/sign-up")
    public ResponseEntity<ApiResponse<Long>> signUp(@Valid @RequestBody SignUpRequest request) {
        Long memberId = authService.signUp(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "회원가입이 완료되었습니다.", memberId));
    }

    /**
     * 일반 로그인
     */
    @Operation(summary = "로그인", description = "이메일과 비밀번호를 통해 로그인하고 인증 토큰을 발급받습니다.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse tokenResponse = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("로그인 성공", tokenResponse));
    }

    /**
     * Access Token 재발급 (Refresh Token Rotation)
     */
    @Operation(summary = "토큰 재발급", description = "Refresh Token을 제출하여 새로운 Access/Refresh 토큰을 재발급받습니다.")
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
    @Operation(summary = "로그아웃", description = "인증된 사용자의 Refresh Token을 DB에서 삭제합니다.")
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
