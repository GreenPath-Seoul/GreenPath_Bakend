package com.seoul.greenpath.domain.course.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 코스와 장소 사이의 다대다 관계를 순서와 함께 관리하는 엔티티
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CourseStop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id")
    private Place place;

    private Integer stopOrder;     // 순서 (1,2,3)
    private Integer stayMinutes;   // 체류 시간

    // ── 연관관계 편의용 ─────────────────────────────────────────
    public void setCourse(Course course) {
        this.course = course;
    }
}
