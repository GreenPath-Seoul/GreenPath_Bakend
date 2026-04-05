package com.seoul.greenpath.domain.course.dto.response;

/**
 * 탐방 종료 후 보여줄 요약 결과 DTO
 */
public record CourseRecordResultResponse(
    Long recordId,
    String courseTitle,
    Summary summary,
    CO2 co2,
    Reward reward,
    BadgeInfo badge
) {
    public record Summary(Double distance, Integer duration, Integer visitedCount) {}
    public record CO2(Double amount, Double treeEquivalent) {}
    public record Reward(Integer basePoint, Integer bonusPoint, Integer totalPoint) {}
    public record BadgeInfo(String code, String name) {}
}
