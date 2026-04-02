package com.seoul.greenpath.domain.member.service;

import com.seoul.greenpath.domain.member.entity.Badge;
import com.seoul.greenpath.domain.member.entity.Member;
import com.seoul.greenpath.domain.member.entity.MemberBadge;
import com.seoul.greenpath.domain.member.repository.BadgeRepository;
import com.seoul.greenpath.domain.member.repository.MemberBadgeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * 사용자 보상(포인트, 레벨, 뱃지) 전담 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RewardService {

    private final BadgeRepository badgeRepository;
    private final MemberBadgeRepository memberBadgeRepository;

    // 레벨 이름 맵 (포인트 하한선 기준)
    private static final TreeMap<Integer, String> LEVEL_NAMES = new TreeMap<>(Map.of(
            0, "새싹 라이더 🌱",
            100, "초보 라이더 🚶",
            300, "그린 라이더 🌿",
            700, "탐험가 🚴",
            1500, "에코 탐험가 🌳",
            3000, "베테랑 라이더 🔥",
            5000, "환경 지킴이 🌍",
            8000, "프로 라이더 🏁",
            12000, "지구 수호자 🌎",
            20000, "레전드 라이더 👑"
    ));

    /**
     * 현재 포인트에 따른 레벨 번호를 계산합니다. (1~10)
     */
    public Integer calculateLevel(Integer totalPoint) {
        if (totalPoint < 100) return 1;
        if (totalPoint < 300) return 2;
        if (totalPoint < 700) return 3;
        if (totalPoint < 1500) return 4;
        if (totalPoint < 3000) return 5;
        if (totalPoint < 5000) return 6;
        if (totalPoint < 8000) return 7;
        if (totalPoint < 12000) return 8;
        if (totalPoint < 20000) return 9;
        return 10;
    }

    /**
     * 현재 포인트에 따른 레벨 명칭을 반환합니다.
     */
    public String getLevelName(Integer totalPoint) {
        return LEVEL_NAMES.floorEntry(totalPoint).getValue();
    }

    /**
     * 새로운 탐방 기록을 바탕으로 획득 가능한 뱃지를 체크하고 부여합니다.
     * (시니어 팁: 뱃지 발급 조건은 추후 뱃지 타입별 팩토리나 전략 패턴으로 분리하기 좋음)
     */
    @Transactional
    public List<Badge> checkAndGiveBadges(Member member) {
        List<Badge> newBadges = new ArrayList<>();

        // 1. 탐색 마스터 뱃지 체크
        checkBadge(member, "FIRST_COURSE", member.getCompletedCourseCount() >= 1, newBadges);
        checkBadge(member, "EXPLORER_3", member.getCompletedCourseCount() >= 3, newBadges);
        checkBadge(member, "BETERAN_10", member.getCompletedCourseCount() >= 10, newBadges);
        checkBadge(member, "MASTER_30", member.getCompletedCourseCount() >= 30, newBadges);

        // 2. 문화재/지식 뱃지 체크
        checkBadge(member, "FIRST_CULTURE", member.getVisitedPlaceCount() >= 1, newBadges);
        checkBadge(member, "CULTURE_EXPLORER_5", member.getVisitedPlaceCount() >= 5, newBadges);
        checkBadge(member, "HISTORY_COLLECT_15", member.getVisitedPlaceCount() >= 15, newBadges);
        checkBadge(member, "CULTURE_MASTER_30", member.getVisitedPlaceCount() >= 30, newBadges);

        // 3. 친환경/CO2 뱃지 체크
        checkBadge(member, "GREEN_1", member.getTotalCo2() >= 1.0, newBadges);
        checkBadge(member, "GREEN_5", member.getTotalCo2() >= 5.0, newBadges);
        checkBadge(member, "ECO_HERO_20", member.getTotalCo2() >= 20.0, newBadges);
        checkBadge(member, "EARTH_GUARD_50", member.getTotalCo2() >= 50.0, newBadges);

        // 4. 거리 뱃지 체크
        checkBadge(member, "DISTANCE_5", member.getTotalDistance() >= 5.0, newBadges);
        checkBadge(member, "DISTANCE_20", member.getTotalDistance() >= 20.0, newBadges);
        checkBadge(member, "ROAD_WARRIOR_50", member.getTotalDistance() >= 50.0, newBadges);
        checkBadge(member, "IRON_RIDER_100", member.getTotalDistance() >= 100.0, newBadges);

        return newBadges;
    }

    private void checkBadge(Member member, String badgeCode, boolean condition, List<Badge> results) {
        if (!condition) return;

        // 이미 가지고 있는지 체크
        boolean hasBadge = memberBadgeRepository.findByMember(member).stream()
                .anyMatch(mb -> mb.getBadge().getCode().equals(badgeCode));

        if (!hasBadge) {
            badgeRepository.findByCode(badgeCode).ifPresent(badge -> {
                memberBadgeRepository.save(MemberBadge.builder()
                        .member(member)
                        .badge(badge)
                        .build());
                results.add(badge);
                log.info("[RewardService] 뱃지 수여 - Member: {}, Badge: {}", member.getEmail(), badge.getName());
            });
        }
    }
}
