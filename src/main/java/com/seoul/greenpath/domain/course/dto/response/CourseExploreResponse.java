package com.seoul.greenpath.domain.course.dto.response;

/**
 * 코스 탐방 중 특정 경유지의 상세 정보 (지도 표시용 및 설명용)
 */
public record CourseExploreResponse(
    Long id,
    String code,
    Long courseId,
    String courseCode,
    String name,
    String description,
    Double latitude,
    Double longitude,
    String imageUrl
) {}
