package com.seoul.greenpath.domain.member.service;

import com.seoul.greenpath.domain.member.dto.request.CourseRequest;
import com.seoul.greenpath.domain.member.entity.Member;
import com.seoul.greenpath.domain.member.entity.MemberPreference;
import com.seoul.greenpath.domain.member.repository.MemberPreferenceRepository;
import com.seoul.greenpath.domain.member.repository.MemberRepository;
import com.seoul.greenpath.global.exception.CustomException;
import com.seoul.greenpath.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberPreferenceService {

    private final MemberPreferenceRepository memberPreferenceRepository;
    private final MemberRepository memberRepository;

    /**
     * 사용자의 취향(선호도) 정보를 저장하거나 업데이트합니다.
     * JPA의 Dirty Checking을 활용하여 기존 정보가 있을 경우 업데이트하고, 없을 경우 새로 생성합니다.
     */
    @Transactional
    public void updatePreference(Long memberId, CourseRequest request) {
        log.info("[MemberPreferenceService] 취향 업데이트 요청 - Member ID: {}", memberId);

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        MemberPreference preference = memberPreferenceRepository.findByMember(member)
                .orElseGet(() -> {
                    log.info("[MemberPreferenceService] 기존 취향 정보 없음, 신규 생성 - Member ID: {}", memberId);
                    return memberPreferenceRepository.save(
                            MemberPreference.builder()
                                    .member(member)
                                    .build()
                    );
                });

        preference.update(
                request.mood(),
                request.duration(),
                request.level(),
                request.location(),
                request.latitude(),
                request.longitude()
        );
        
        log.info("[MemberPreferenceService] 취향 업데이트 성공 - Member ID: {}", memberId);
    }

    /**
     * 사용자의 현재 설정된 취향 정보를 조회합니다. (필요 시 활용)
     */
    public MemberPreference getPreference(Long memberId) {
        return memberPreferenceRepository.findByMemberId(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
