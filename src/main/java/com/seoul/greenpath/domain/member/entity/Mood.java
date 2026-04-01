package com.seoul.greenpath.domain.member.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 사용자 선호 분위기
 */
@Getter
@RequiredArgsConstructor
public enum Mood {
    QUIET("조용한"),
    PHOTO("사진 찍기 좋은"),
    HISTORY("역사 탐방"),
    HIP("힙한");

    private final String description;
}
