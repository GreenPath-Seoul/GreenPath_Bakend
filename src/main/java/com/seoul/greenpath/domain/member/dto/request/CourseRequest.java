package com.seoul.greenpath.domain.member.dto.request;

import com.seoul.greenpath.domain.member.entity.Mood;
import com.seoul.greenpath.domain.member.entity.Duration;
import com.seoul.greenpath.domain.member.entity.Level;
import com.seoul.greenpath.domain.member.entity.Location;

/**
 * 코스 추천 요청 및 사용자 취향 저장을 위한 DTO
 */
public record CourseRequest(
    Mood mood,
    Duration duration,
    Level level,
    Location location,
    Double latitude,
    Double longitude,
    String preferenceText
) {}
