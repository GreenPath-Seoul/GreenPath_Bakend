package com.seoul.greenpath.domain.member.dto.response;

import java.util.List;

/**
 * 마이페이지 조회 응답 DTO
 */
public record MyPageResponse(
    UserInfo user,
    Stats stats,
    List<BadgeInfo> badges,
    List<RecentRecord> recentRecords
) {
    public record UserInfo(String name, Integer level, String levelName) {}
    public record Stats(Double totalCo2, Double totalDistance, Integer visitedCount, Integer totalPoint) {}
    public record BadgeInfo(String code, String name) {}
    public record RecentRecord(Long recordId, String title, String date, Integer point) {}
}
