package com.seoul.greenpath.domain.course.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 코스와 장소 사이의 다대다 관계를 순서와 함께 관리하는 엔티티
 */
@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CourseStop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id")
    private Place place;

    private Integer stopOrder;     // 순서 (1,2,3)
    private Integer stayMinutes;   // 체류 시간
    private Double distanceFromPrev; // 이전 장소와의 거리 (km)
    private Double durationFromPrev; // 이전 장소까지의 예상 소요 시간 (분)

    // ── 연관관계 편의용 ─────────────────────────────────────────
    public void setCourse(Course course) {
        this.course = course;
    }
}
