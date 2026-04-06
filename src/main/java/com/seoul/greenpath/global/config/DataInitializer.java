package com.seoul.greenpath.global.config;

import com.seoul.greenpath.domain.member.entity.Badge;
import com.seoul.greenpath.domain.member.repository.BadgeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 서비스 초기 데이터(뱃지 등)를 주입합니다.
 */
@Component
@RequiredArgsConstructor
public class DataInitializer {

    private final BadgeRepository badgeRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Order(1)
    public void initData() {
        if (badgeRepository.count() > 0) return;

        List<Badge> defaultBadges = List.of(
            // 탐색 관련
            Badge.builder().code("FIRST_COURSE").name("첫 한옥 탐방").description("첫 코스 탐방을 완료하였습니다.").build(),
            Badge.builder().code("EXPLORER_3").name("초보 탐험가").description("3개의 코스를 완료하였습니다.").build(),
            Badge.builder().code("BETERAN_10").name("베테랑 라이더").description("10개의 코스를 완료하였습니다.").build(),
            Badge.builder().code("MASTER_30").name("마스터 라이더").description("30개의 코스를 완료하였습니다.").build(),
            
            // 문화재 관련
            Badge.builder().code("FIRST_CULTURE").name("한옥 입문자").description("첫 문화재를 방문하였습니다.").build(),
            Badge.builder().code("CULTURE_EXPLORER_5").name("문화 탐험가").description("5곳의 문화재를 탐방하였습니다.").build(),
            Badge.builder().code("HISTORY_COLLECT_15").name("역사 수집가").description("15곳의 문화재 명소를 수집하였습니다.").build(),
            Badge.builder().code("CULTURE_MASTER_30").name("문화 마스터").description("30곳의 역사 문화재를 완벽히 마스터했습니다.").build(),

            // 환경 관련
            Badge.builder().code("GREEN_1").name("환경 새싹").description("CO2 1kg를 줄였습니다.").build(),
            Badge.builder().code("GREEN_5").name("그린 라이더").description("CO2 5kg를 줄였습니다.").build(),
            Badge.builder().code("ECO_HERO_20").name("에코 히어로").description("환경 보호에 앞장서는 CO2 20kg 절감 업적.").build(),
            Badge.builder().code("EARTH_GUARD_50").name("지구 수호자").description("지구 환경을 위해 CO2 50kg 이상 절감.").build()
        );

        badgeRepository.saveAll(defaultBadges);
    }
}
