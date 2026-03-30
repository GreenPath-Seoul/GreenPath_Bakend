package com.seoul.greenpath.domain.member.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    // JWT 기반 일반 로그인 사용자를 위한 비밀번호 (OAuth2 가입자는 null일 수 있음)
    private String password;

    @Column(nullable = false)
    private String name;

    // KAKAO, NAVER, LOCAL 등 로그인 제공자 정보
    private String provider;
    
    // 소셜 로그인에서 제공해주는 고유 ID (일반 로그인은 null)
    private String providerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    public void updateName(String name) {
        this.name = name;
    }
}
