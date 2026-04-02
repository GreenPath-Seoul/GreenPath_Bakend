package com.seoul.greenpath.domain.member.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 회원가입 성공 시 발생하는 이벤트 파일
 */
@Getter
@RequiredArgsConstructor
public class MemberRegisteredEvent {
    private final Long memberId;
    private final String email;
    private final String name;
}
