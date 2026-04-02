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

    // ── 연관관계 편의 메서드 ─────────────────────────────────────────
    public void addStop(CourseStop stop) {
        this.stops.add(stop);
        stop.setCourse(this);
    }
}
