package com.seoul.greenpath.domain.member.repository;

import com.seoul.greenpath.domain.member.entity.Member;
import com.seoul.greenpath.domain.member.entity.MemberPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberPreferenceRepository extends JpaRepository<MemberPreference, Long> {
    Optional<MemberPreference> findByMember(Member member);
    Optional<MemberPreference> findByMemberId(Long memberId);
}
