package com.seoul.greenpath.global.security;

import com.seoul.greenpath.domain.member.entity.Member;
import com.seoul.greenpath.domain.member.repository.MemberRepository;
import com.seoul.greenpath.global.exception.CustomException;
import com.seoul.greenpath.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Security 일반 로그인 처리 시 사용자 조회 서비스
 * - email 기반 조회 (UserDetailsService 표준 구현)
 * - memberId 기반 조회 (JWT 필터 내부 사용)
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    /**
     * 이메일로 사용자 조회 (Spring Security 표준 흐름)
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("이메일을 찾을 수 없습니다: " + email));
        return new CustomUserDetails(member);
    }

    /**
     * ID로 사용자 조회 (JWT 필터에서 호출)
     */
    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
        return new CustomUserDetails(member);
    }
}
