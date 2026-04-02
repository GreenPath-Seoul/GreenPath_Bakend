package com.seoul.greenpath.global.oauth2.userinfo;

import java.util.Map;

/**
 * OAuth2 제공자별 사용자 정보 추상 클래스
 *
 * 카카오 / 네이버는 응답 구조가 달라서 각각 별도 구현체로 처리
 * → AuthService는 이 인터페이스만 바라보므로 OCP 준수
 */
public abstract class OAuth2UserInfo {

    protected final Map<String, Object> attributes;

    protected OAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    /**
     * OAuth2 제공자가 부여한 고유 ID
     * (ex. 카카오: 숫자 id, 네이버: id 필드)
     */
    public abstract String getId();

    /** 사용자 이름 (닉네임) */
    public abstract String getName();

    /** 이메일 (제공자에 따라 null 가능) */
    public abstract String getEmail();

    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
