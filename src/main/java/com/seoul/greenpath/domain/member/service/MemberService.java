package com.seoul.greenpath.domain.member.service;

import com.seoul.greenpath.domain.course.repository.ExploreRecordRepository;
import com.seoul.greenpath.domain.member.dto.response.MyPageResponse;
import com.seoul.greenpath.domain.member.entity.Member;
import com.seoul.greenpath.domain.member.repository.MemberBadgeRepository;
import com.seoul.greenpath.domain.member.repository.MemberRepository;
import com.seoul.greenpath.global.exception.CustomException;
import com.seoul.greenpath.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 회원 관련 부가 서비스 (마이페이지 등)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final MemberBadgeRepository memberBadgeRepository;
    private final ExploreRecordRepository exploreRecordRepository;
    private final RewardService rewardService;

    /**
     * 사용자의 마이페이지 정보를 조회합니다.
     */
    public MyPageResponse getMyPage(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        // 1. 사용자 기본 정보 및 레벨 명칭
        MyPageResponse.UserInfo userInfo = new MyPageResponse.UserInfo(
                member.getName(),
                rewardService.calculateLevel(member.getTotalPoint()),
                rewardService.getLevelName(member.getTotalPoint())
        );

        // 2. 누적 통계 데이터
        MyPageResponse.Stats stats = new MyPageResponse.Stats(
                member.getTotalCo2(),
                member.getTotalDistance(),
                member.getVisitedPlaceCount(),
                member.getTotalPoint()
        );

        // 3. 보유한 전체 뱃지 목록
        List<MyPageResponse.BadgeInfo> badges = memberBadgeRepository.findByMember(member).stream()
                .map(mb -> new MyPageResponse.BadgeInfo(mb.getBadge().getCode(), mb.getBadge().getName()))
                .collect(Collectors.toList());

        // 4. 최근 5건의 탐방 기록
        List<MyPageResponse.RecentRecord> recentRecords = exploreRecordRepository.findTop5ByMemberOrderByCreatedAtDesc(member).stream()
                .map(record -> new MyPageResponse.RecentRecord(
                        record.getId(),
                        record.getCourse().getTitle(),
                        record.getCreatedAt().toLocalDate().toString(),
                        record.getTotalPoint()
                ))
                .collect(Collectors.toList());

        return new MyPageResponse(userInfo, stats, badges, recentRecords);
    }
}
