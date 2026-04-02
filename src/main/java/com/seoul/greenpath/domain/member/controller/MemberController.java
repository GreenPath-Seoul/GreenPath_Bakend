package com.seoul.greenpath.domain.member.controller;

import com.seoul.greenpath.domain.member.dto.response.MyPageResponse;
import com.seoul.greenpath.domain.member.service.MemberService;
import com.seoul.greenpath.global.common.ApiResponse;
import com.seoul.greenpath.global.security.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "Member", description = "회원 정보 관련 API")
@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    /**
     * 현재 로그인한 사용자의 마이페이지 정보를 조회합니다.
     */
    @Operation(summary = "마이페이지 조회", description = "사용자의 레벨, 누적 통계, 획득 뱃지 및 최근 기록을 조회합니다.")
    @GetMapping("/me/mypage")
    public ApiResponse<MyPageResponse> getMyPage() {
        log.info("[MemberController] 마이페이지 조회 요청");
        Long memberId = SecurityUtil.getCurrentMemberId();
        MyPageResponse response = memberService.getMyPage(memberId);
        return ApiResponse.success("마이페이지 정보를 성공적으로 조회했습니다.", response);
    }
}
