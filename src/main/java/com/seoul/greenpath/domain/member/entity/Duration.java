package com.seoul.greenpath.domain.member.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 소요 시간 선호도
 */
@Getter
@RequiredArgsConstructor
public enum Duration {
    ONE_HOUR("1시간"),
    TWO_HOURS("2시간"),
    HALF_DAY("반나절");

    private final String description;
}
