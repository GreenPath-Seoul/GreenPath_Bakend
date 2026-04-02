package com.seoul.greenpath.domain.member.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 코스 난이도 선호도
 */
@Getter
@RequiredArgsConstructor
public enum Level {
    EASY("쉬움"),
    MEDIUM("보통"),
    ANY("상관없음");

    private final String description;
}
