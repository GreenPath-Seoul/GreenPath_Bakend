package com.seoul.greenpath.domain.member.repository;

import com.seoul.greenpath.domain.member.entity.Badge;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BadgeRepository extends JpaRepository<Badge, Long> {
    Optional<Badge> findByCode(String code);
}
