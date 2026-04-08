package com.seoul.greenpath.domain.weather.entity;

import com.seoul.greenpath.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 실시간 날씨 정보 엔티티
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Weather extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String temp;        // 기온 (T1H)
    private String rainType;    // 강수형태 (PTY): 0(없음), 1(비), 2(비/눈), 3(눈), 5(빗방울), 6(빗방울눈날림), 7(눈날림)
    private String rainAmount;  // 1시간 강수량 (RN1)
    private String humidity;    // 습도 (REH)
    private String skyStatus;   // 하늘상태 (SKY) - 초단기실황에는 없으나 단기예보 참고용으로 남겨둠

    private String baseDate;    // 발표 일자
    private String baseTime;    // 발표 시각
}
