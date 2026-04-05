package com.seoul.greenpath.domain.course.controller;

import com.seoul.greenpath.domain.course.dto.request.CourseCompleteRequest;
import com.seoul.greenpath.domain.course.dto.response.CourseExploreResponse;
import com.seoul.greenpath.domain.course.dto.response.CourseRecordResultResponse;
import com.seoul.greenpath.domain.course.dto.response.CourseResponse;
import com.seoul.greenpath.domain.course.service.CourseService;
import com.seoul.greenpath.domain.member.dto.request.CourseRequest;
import com.seoul.greenpath.global.common.ApiResponse;
import com.seoul.greenpath.global.security.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 코스 추천 관련 컨트롤러
 */
@Slf4j
@Tag(name = "Course", description = "코스 추천 및 조회 API")
@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    /**
     * 사용자의 선호 옵션을 받아 맞춤 코스를 추천합니다. (Mock 데이터)
     */
    @Operation(summary = "맞춤 코스 추천", description = "분위기, 소요 시간, 난이도 등 유저 설정에 맞는 AI 추천 코스를 제안합니다.")
    @PostMapping("/recommend")
    public ApiResponse<List<CourseResponse>> recommend(@RequestBody(required = false) CourseRequest request) {
        log.info("[CourseController] 코스 추천 요청 수신");
        
        Long memberId = SecurityUtil.getCurrentMemberId();
        List<CourseResponse> response = courseService.getRecommendCourses(memberId, request);
        
        return ApiResponse.success("성공적으로 분석된 코스를 반환하였습니다.", response);
    }
    @Operation(summary = "코스 상세 정보 조회", description = "선택한 코스에 대한 정보를 반환합니다.")
    @GetMapping("/{courseId}")
    public ApiResponse<CourseResponse> getCourseDetail(@PathVariable Long courseId) {
        log.info("[CourseController] 코스 상세 조회 요청 수신");

        CourseResponse response = courseService.getCourseById(courseId);

        return ApiResponse.success("코스 정보를 성공적으로 조회했습니다.", response);
    }
    /**
     * 탐방 중(실시간) 특정 경유지의 상세 정보(위치, 설명 등)를 조회합니다.
     */
    @Operation(summary = "경유지 상세 정보 조회", description = "탐방 중인 코스의 특정 순서 경유지 정보를 가져옵니다.")
    @GetMapping("/{courseId}/stops/{stopOrder}")
    public ApiResponse<CourseExploreResponse> getCourseStopInfo(
            @PathVariable Long courseId,
            @PathVariable Integer stopOrder) {
        log.info("[CourseController] 경유지 상세 정보 요청 - Course: {}, Order: {}", courseId, stopOrder);
        
        CourseExploreResponse response = courseService.getCourseStopInfo(courseId, stopOrder);
        
        return ApiResponse.success("경유지 정보를 성공적으로 조회하였습니다.", response);
    }

    /**
     * 탐방 종료 후 결과를 전송하고 보상을 획득합니다.
     */
    @Operation(summary = "탐방 완료 처리", description = "탐방 종료 시 주행 거리 및 방문 정보를 기록하고 포인트를 획득합니다.")
    @PostMapping("/complete")
    public ApiResponse<CourseRecordResultResponse> completeExploration(@RequestBody CourseCompleteRequest request) {
        log.info("[CourseController] 탐방 완료 요청 수신");
        Long memberId = SecurityUtil.getCurrentMemberId();
        CourseRecordResultResponse result = courseService.completeExploration(memberId, request);
        return ApiResponse.success("탐방 결과가 기록되고 보상이 지급되었습니다.", result);
    }

    /**
     * 특정 탐방 기록의 상세 결과를 조회합니다.
     */
    @Operation(summary = "탐방 기록 상세 조회", description = "기록 ID를 통해 과거 탐방 결과를 다시 조회합니다.")
    @GetMapping("/records/{recordId}")
    public ApiResponse<CourseRecordResultResponse> getExploreRecordResult(@PathVariable Long recordId) {
        log.info("[CourseController] 탐방 기록 상세 조회 요청 수신 - Record: {}", recordId);
        Long memberId = SecurityUtil.getCurrentMemberId();
        CourseRecordResultResponse result = courseService.getExploreRecordResult(memberId, recordId);
        return ApiResponse.success("탐방 기록을 성공적으로 조회하였습니다.", result);
    }
}
