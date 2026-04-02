package com.seoul.greenpath.domain.member.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 획득 가능한 뱃지 마스터 엔티티
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Badge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;
}
