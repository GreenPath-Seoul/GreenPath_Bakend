package com.seoul.greenpath.domain.member.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 장소 선호 범위
 */
@Getter
@RequiredArgsConstructor
public enum Location {
    NEARBY("현재 위치 주변"),
    ANY("상관없음");

    private final String description;
}
