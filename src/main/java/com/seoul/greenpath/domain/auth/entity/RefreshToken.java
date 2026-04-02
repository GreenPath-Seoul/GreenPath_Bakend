package com.seoul.greenpath.domain.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Refresh Token 저장 엔티티
 * - DB 기반으로 관리하여 서버 재시작 후에도 유효성 보장
 * - 로그아웃 / 토큰 탈취 대응을 위한 블랙리스트/회전 전략 지원
 */
@Entity
@Table(name = "refresh_token", indexes = {
        @Index(name = "idx_refresh_token_member_id", columnList = "member_id"),
        @Index(name = "idx_refresh_token_token", columnList = "token")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 다중 기기 로그인 지원을 위해 memberId 별로 복수 토큰 허용 가능
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(nullable = false, unique = true, length = 512)
    private String token;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    // 마지막 사용 IP (보안 감사 로그용)
    @Column(length = 45)
    private String lastUsedIp;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    /**
     * 토큰 갱신 시 새 토큰값으로 교체 (Token Rotation 전략)
     */
    public void rotate(String newToken, LocalDateTime newExpiresAt, String currentIp) {
        this.token = newToken;
        this.expiresAt = newExpiresAt;
        this.lastUsedIp = currentIp;
    }
}
