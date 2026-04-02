package com.seoul.greenpath.global.security;

import com.seoul.greenpath.domain.member.entity.Member;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Spring Security 인증 주체 (UserDetails + OAuth2User 통합)
 *
 * 일반 로그인(JWT) + OAuth2 로그인 모두 동일한 Principal 타입 사용
 * → Controller에서 @AuthenticationPrincipal CustomUserDetails로 일관성 있게 주입
 */
@Getter
public class CustomUserDetails implements UserDetails, OAuth2User {

    private final Long memberId;
    private final String email;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;
    private Map<String, Object> attributes;  // OAuth2 전용

    // ── 일반 로그인용 생성자 ──────────────────────────────────
    public CustomUserDetails(Member member) {
        this.memberId   = member.getId();
        this.email      = member.getEmail();
        this.password   = member.getPassword();
        this.authorities = Collections.singletonList(
                new SimpleGrantedAuthority(member.getRole().getKey())
        );
    }

    // ── OAuth2 로그인용 생성자 ────────────────────────────────
    public CustomUserDetails(Member member, Map<String, Object> attributes) {
        this(member);
        this.attributes = attributes;
    }

    // ── OAuth2User 구현 ──────────────────────────────────────
    @Override
    public Map<String, Object> getAttributes() {
        return attributes != null ? attributes : Collections.emptyMap();
    }

    @Override
    public String getName() {
        return String.valueOf(memberId);
    }

    // ── UserDetails 구현 ─────────────────────────────────────
    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override public boolean isAccountNonExpired()  { return true; }
    @Override public boolean isAccountNonLocked()   { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()            { return true; }
}
