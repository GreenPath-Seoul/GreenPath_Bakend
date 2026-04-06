package com.seoul.greenpath.domain.member.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 사용자 개인 맞춤 코스 추천을 위한 선호도 엔티티
 */
@Entity
@Table(name = "member_preference")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MemberPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Mood mood;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Duration duration;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Level level;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Location location;

    @Column(columnDefinition = "TEXT")
    private String preferenceText;

    // H2(로컬)와 PostgreSQL(운영) 호환을 위해 columnDefinition 제거
    // 운영 환경(PostgreSQL)에서는 dialect 등을 통해 vector 타입으로 매핑되도록 처리 가능합니다.
    private float[] embedding;

    private Double latitude;
    private Double longitude;

    // ── 도메인 메서드 ─────────────────────────────────────────

    public void update(Mood mood, Duration duration, Level level, Location location, Double latitude, Double longitude, String preferenceText, float[] embedding) {
        this.mood = mood;
        this.duration = duration;
        this.level = level;
        this.location = location;
        this.latitude = latitude;
        this.longitude = longitude;
        this.preferenceText = preferenceText;
        if (embedding != null) {
            this.embedding = embedding;
        }
    }
}
