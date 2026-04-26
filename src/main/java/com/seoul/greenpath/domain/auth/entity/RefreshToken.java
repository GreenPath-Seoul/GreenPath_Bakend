package com.seoul.greenpath.domain.auth.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;
import org.springframework.data.redis.core.TimeToLive;

import java.time.LocalDateTime;

/**
 * Refresh Token 저장 엔티티 (Redis 기반)
 * - Redis를 활용하여 캐시에 저장하고 TTL을 통해 자동 만료 관리
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@RedisHash(value = "refreshToken")
public class RefreshToken {

    @Id
    private String token;

    @Indexed
    private Long memberId;

    private LocalDateTime expiresAt;

    private String lastUsedIp;

    private LocalDateTime createdAt;

    @TimeToLive
    private Long ttl;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }
}
