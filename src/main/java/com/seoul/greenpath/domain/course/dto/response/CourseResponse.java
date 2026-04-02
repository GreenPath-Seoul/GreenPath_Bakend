package com.seoul.greenpath.domain.course.dto.response;

import com.seoul.greenpath.domain.member.entity.Level;
import java.util.List;
import java.time.LocalDateTime;

public record CourseResponse(
    Long courseId,
    String title,
    String description,
    CourseSummary summary,
    List<CourseStop> stops,
    String polyline,
    LocalDateTime createdAt
) {
    public record CourseSummary(
        Double distanceKm,
        Integer durationMinutes,
        Level difficulty,
        Double carbonReductionKg
    ) {}

    public record CourseStop(
        Integer order,
        String name,
        String description,
        Integer stayMinutes,
        Double latitude,
        Double longitude,
        String imageUrl
    ) {}
}
