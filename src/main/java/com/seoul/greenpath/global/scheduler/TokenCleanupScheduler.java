package com.seoul.greenpath.global.scheduler;

import com.seoul.greenpath.domain.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 만료된 Refresh Token 정리 스케줄러
 *
 * - 기본 매일 새벽 3시 실행 (cron 조정 가능)
 * - DB에 쌓이는 만료 토큰을 주기적으로 정리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenCleanupScheduler {

    private final RefreshTokenRepository refreshTokenRepository;

    @Scheduled(cron = "0 0 3 * * *")   // 매일 03:00
    @Transactional
    public void cleanupExpiredTokens() {
        int before = (int) refreshTokenRepository.count();
        refreshTokenRepository.deleteAllExpiredTokens(LocalDateTime.now());
        int after = (int) refreshTokenRepository.count();
        log.info("[TokenCleanup] 만료 토큰 {}건 삭제 완료", before - after);
    }
}
