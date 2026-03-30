package com.seoul.greenpath.global.oauth2.userinfo;

import com.seoul.greenpath.global.exception.CustomException;
import com.seoul.greenpath.global.exception.ErrorCode;

import java.util.Map;

/**
 * registrationId ("kakao" | "naver") 에 따라 적절한 OAuth2UserInfo 구현체를 반환
 */
public class OAuth2UserInfoFactory {

    private OAuth2UserInfoFactory() {}

    public static OAuth2UserInfo of(String registrationId, Map<String, Object> attributes) {
        return switch (registrationId.toLowerCase()) {
            case "kakao" -> new KakaoOAuth2UserInfo(attributes);
            case "naver" -> new NaverOAuth2UserInfo(attributes);
            default -> throw new CustomException(ErrorCode.OAUTH2_PROVIDER_NOT_SUPPORTED);
        };
    }
}
