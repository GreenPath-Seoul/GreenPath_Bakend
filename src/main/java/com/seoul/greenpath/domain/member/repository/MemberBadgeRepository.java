package com.seoul.greenpath.domain.member.repository;

import com.seoul.greenpath.domain.member.entity.Member;
import com.seoul.greenpath.domain.member.entity.MemberBadge;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MemberBadgeRepository extends JpaRepository<MemberBadge, Long> {
    List<MemberBadge> findByMember(Member member);
}
