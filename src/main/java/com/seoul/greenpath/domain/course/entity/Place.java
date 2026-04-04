package com.seoul.greenpath.domain.course.entity;

import com.seoul.greenpath.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 코스 내 경유지(장소) 정보
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Place extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String code;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    private String imageUrl;

    private String category; // 문화재, 카페, 유적지 등
}
