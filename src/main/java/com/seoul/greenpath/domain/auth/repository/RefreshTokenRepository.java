package com.seoul.greenpath.domain.auth.repository;

import com.seoul.greenpath.domain.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    Optional<RefreshToken> findByMemberId(Long memberId);

    boolean existsByToken(String token);

    void deleteByMemberId(Long memberId);

    void deleteByToken(String token);

    /**
     * 만료된 토큰 일괄 삭제 (스케줄러에서 주기적 호출)
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now")
    void deleteAllExpiredTokens(@Param("now") LocalDateTime now);
}
