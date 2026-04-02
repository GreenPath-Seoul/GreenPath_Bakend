package com.seoul.greenpath.global.oauth2.userinfo;

import java.util.Map;

/**
 * 네이버 OAuth2 사용자 정보 파싱
 *
 * 네이버 응답 구조:
 * {
 *   "resultcode": "00",
 *   "message": "success",
 *   "response": {
 *     "id": "uniqueId",
 *     "email": "user@naver.com",
 *     "name": "홍길동"
 *   }
 * }
 *
 * Spring Security OAuth2는 "response" 안의 내용을 attributes로 전달하지 않으므로
 * registrationId를 "naver"로 지정하고 user-name-attribute: response 설정 필요
 */
public class NaverOAuth2UserInfo extends OAuth2UserInfo {

    public NaverOAuth2UserInfo(Map<String, Object> attributes) {
        // 네이버는 user-name-attribute가 "response"이므로 attributes 안에
        // "response" 맵이 들어있음. 편의상 최상위 맵 그대로 사용.
        super(attributes);
    }

    @Override
    @SuppressWarnings("unchecked")
    public String getId() {
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");
        if (response == null) return null;
        return (String) response.get("id");
    }

    @Override
    @SuppressWarnings("unchecked")
    public String getName() {
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");
        if (response == null) return null;
        return (String) response.get("name");
    }

    @Override
    @SuppressWarnings("unchecked")
    public String getEmail() {
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");
        if (response == null) return null;
        return (String) response.get("email");
    }
}
