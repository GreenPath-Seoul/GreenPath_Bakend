package com.seoul.greenpath.domain.auth.repository;

import com.seoul.greenpath.domain.auth.entity.RefreshToken;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends CrudRepository<RefreshToken, String> {

    Optional<RefreshToken> findByToken(String token);

    Optional<RefreshToken> findByMemberId(Long memberId);

    boolean existsByToken(String token);

    void deleteByMemberId(Long memberId);

    void deleteByToken(String token);
}
