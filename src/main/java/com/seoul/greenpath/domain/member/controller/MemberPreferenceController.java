package com.seoul.greenpath.domain.member.controller;

import com.seoul.greenpath.domain.member.dto.request.CourseRequest;
import com.seoul.greenpath.domain.member.service.MemberPreferenceService;
import com.seoul.greenpath.global.common.ApiResponse;
import com.seoul.greenpath.global.security.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "MemberPreference", description = "사용자 취향 설정 관련 API")
@RestController
@RequestMapping("/api/v1/members/preferences")
@RequiredArgsConstructor
public class MemberPreferenceController {

    private final MemberPreferenceService memberPreferenceService;

    /**
     * 사용자의 취향(선호도) 정보를 저장하거나 업데이트합니다.
     */
    @Operation(summary = "사용자 취향 설정/업데이트", description = "분위기, 소급시간, 난이도 및 좌표를 저장합니다.")
    @PostMapping
    public ApiResponse<String> savePreference(@RequestBody CourseRequest request) {
        log.info("[MemberPreferenceController] 취향 저장 요청 수신");
        
        Long memberId = SecurityUtil.getCurrentMemberId();
        memberPreferenceService.updatePreference(memberId, request);
        
        return ApiResponse.success("취향 정보가 성공적으로 저장되었습니다.");
    }
}
