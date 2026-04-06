package com.seoul.greenpath.domain.course.entity;

import com.seoul.greenpath.domain.member.entity.Level;
import com.seoul.greenpath.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 추천된 코스 핵심 정보
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Course extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String code;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Double distanceKm;
    private Integer durationMinutes;

    @Enumerated(EnumType.STRING)
    private Level difficulty;

    private Double carbonReductionKg;

    @Column(columnDefinition = "TEXT")
    private String polyline;

    @Builder.Default
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CourseStop> stops = new ArrayList<>();

    private double healingScore;
    private double emotionalScore;
    private double historicalScore;
    private double trendyScore;     // 트렌디

    @Column(columnDefinition = "TEXT")
    private String embeddingText;

    // MySQL/H2 호환을 위해 MEDIUMBLOB 사용 (1536차원 float[] 약 6KB 수용)
    // 운영 환경(PostgreSQL) 전환 시 columnDefinition = "vector(1536)"으로 복약 필요
    @Column(columnDefinition = "MEDIUMBLOB")
    private float[] embedding;

    // ── 연관관계 편의 메서드 ─────────────────────────────────────────
    public void addStop(CourseStop stop) {
        this.stops.add(stop);
        stop.setCourse(this);
    }
}
