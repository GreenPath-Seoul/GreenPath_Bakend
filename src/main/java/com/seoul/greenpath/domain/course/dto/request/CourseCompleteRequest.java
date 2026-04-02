package com.seoul.greenpath.domain.course.dto.request;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 탐방 종료 시 결과 전송을 위한 DTO
 */
public record CourseCompleteRequest(
    Long courseId,
    OffsetDateTime startTime,
    OffsetDateTime endTime,
    List<Long> visitedSpotIds,
    Double distance
) {}
