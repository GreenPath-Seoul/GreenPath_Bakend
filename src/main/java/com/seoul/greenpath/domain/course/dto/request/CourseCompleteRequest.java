package com.seoul.greenpath.domain.course.dto.request;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 탐방 종료 시 결과 전송을 위한 DTO
 */
public record CourseCompleteRequest(
    Long courseId,
    LocalDateTime startTime,
    LocalDateTime endTime,
    List<Long> visitedSpotIds,
    Double distance
) {}
