package com.seoul.greenpath.domain.course.service;

import com.seoul.greenpath.domain.course.dto.request.CourseCompleteRequest;
import com.seoul.greenpath.domain.course.dto.response.CourseExploreResponse;
import com.seoul.greenpath.domain.course.dto.response.CourseRecordResultResponse;
import com.seoul.greenpath.domain.course.dto.response.CourseResponse;
import com.seoul.greenpath.domain.course.entity.Course;
import com.seoul.greenpath.domain.course.entity.CourseStop;
import com.seoul.greenpath.domain.course.entity.ExploreRecord;
import com.seoul.greenpath.domain.course.entity.Place;
import com.seoul.greenpath.domain.course.repository.CourseRepository;
import com.seoul.greenpath.domain.course.repository.CourseStopRepository;
import com.seoul.greenpath.domain.course.repository.ExploreRecordRepository;
import com.seoul.greenpath.domain.member.dto.request.CourseRequest;
import com.seoul.greenpath.domain.member.entity.*;
import com.seoul.greenpath.domain.member.repository.MemberPreferenceRepository;
import com.seoul.greenpath.domain.member.repository.MemberRepository;
import com.seoul.greenpath.domain.member.service.RewardService;
import com.seoul.greenpath.global.exception.CustomException;
import com.seoul.greenpath.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 코스 추천 및 분석 서비스
 * AI 분석 결과를 데이터베이스에 저장하고 영속화합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;
    private final CourseStopRepository courseStopRepository;
    private final ExploreRecordRepository exploreRecordRepository;
    private final MemberRepository memberRepository;
    private final MemberPreferenceRepository memberPreferenceRepository;
    private final RewardService rewardService;

    /**
     * AI 분석 기반 코스를 추천하고 이를 데이터베이스에 저장합니다.
     */
    /**
     * 사용자의 선호 옵션 및 현재 위치를 기반으로 코스를 추천하고, 이번 요청의 조건을 사용자의 기본 선호도로 저장합니다.
     */
    @Transactional
    public List<CourseResponse> getRecommendCourses(Long memberId, CourseRequest request) {
        log.info("[CourseService] 맞춤 코스 추천 및 선호도 업데이트 시작 - Member: {}", memberId);

        MemberPreference preference = updateMemberPreference(memberId, request);

        List<Course> allCourses = courseRepository.findAll();
        if (allCourses.isEmpty()) return new ArrayList<>();

        // 1. 점수 계산 및 거리 계산
        List<ScoredCourse> scoredCourses = allCourses.stream()
                .map(course -> {
                    double score = calculateScore(course, request, preference);
                    double distance = calculateDistanceToFirstStop(course, 
                            (request != null && request.latitude() != null) ? request.latitude() : (preference != null ? preference.getLatitude() : null),
                            (request != null && request.longitude() != null) ? request.longitude() : (preference != null ? preference.getLongitude() : null));
                    return new ScoredCourse(course, score, distance);
                })
                .collect(Collectors.toList());

        // 2. 위치 기반 필터링 (10km 이내)
        List<ScoredCourse> nearbyCourses = scoredCourses.stream()
                .filter(sc -> sc.distance <= 10.0)
                .sorted((a, b) -> Double.compare(b.score, a.score))
                .collect(Collectors.toList());

        List<ScoredCourse> finalSelection;
        if (!nearbyCourses.isEmpty()) {
            finalSelection = nearbyCourses;
        } else {
            // 10km 이내에 없으면 전체에서 점수 높은 순
            finalSelection = scoredCourses.stream()
                    .sorted((a, b) -> Double.compare(b.score, a.score))
                    .collect(Collectors.toList());
        }

        // 3. 상위 3개 반환
        return finalSelection.stream()
                .limit(3)
                .map(sc -> fromEntity(sc.course))
                .collect(Collectors.toList());
    }

    @Transactional
    protected MemberPreference updateMemberPreference(Long memberId, CourseRequest request) {
        if (memberId == null || request == null) return null;

        Member member = memberRepository.findById(memberId)
                .orElse(null);
        if (member == null) return null;

        MemberPreference preference = memberPreferenceRepository.findByMemberId(memberId)
                .orElseGet(() -> MemberPreference.builder().member(member).build());

        preference.update(
                request.mood(),
                request.duration(),
                request.level(),
                request.location(),
                request.latitude(),
                request.longitude()
        );

        return memberPreferenceRepository.save(preference);
    }

    private double calculateScore(Course course, CourseRequest request, MemberPreference preference) {
        double score = 0.0;
        
        // 1. 분위기 (Mood) 가중치
        com.seoul.greenpath.domain.member.entity.Mood targetMood = 
                (request != null && request.mood() != null) ? request.mood() : (preference != null ? preference.getMood() : null);

        if (targetMood != null) {
            score += switch (targetMood) {
                case QUIET -> course.getHealingScore();
                case PHOTO -> course.getEmotionalScore();
                case HISTORY -> course.getHistoricalScore();
                case HIP -> course.getTrendyScore();
            };
        }

        // 2. 소요 시간 (Duration) 매칭 (가중치 10점 - 중요도 상향)
        com.seoul.greenpath.domain.member.entity.Duration targetDuration = 
                (request != null && request.duration() != null) ? request.duration() : (preference != null ? preference.getDuration() : null);

        if (targetDuration != null) {
            int minutes = course.getDurationMinutes();
            boolean match = switch (targetDuration) {
                case ONE_HOUR -> minutes <= 60;
                case TWO_HOURS -> minutes > 60 && minutes <= 120;
                case HALF_DAY -> minutes > 120;
            };
            if (match) score += 10.0;
        }

        // 3. 난이도 (Level) 매칭 (가중치 5점)
        com.seoul.greenpath.domain.member.entity.Level targetLevel = 
                (request != null && request.level() != null) ? request.level() : (preference != null ? preference.getLevel() : null);

        if (targetLevel != null) {
            if (targetLevel == course.getDifficulty() || course.getDifficulty() == com.seoul.greenpath.domain.member.entity.Level.ANY) {
                score += 5.0;
            }
        }
        
        // 4. 평소 선호 스타일 보너스 (저장된 선호도와 일치하는 코스 부각)
        if (preference != null && preference.getMood() != null) {
            if (matchesMood(course, preference.getMood())) score += 3.0;
        }

        return score;
    }

    private boolean matchesMood(Course course, com.seoul.greenpath.domain.member.entity.Mood mood) {
        return switch (mood) {
            case QUIET -> course.getHealingScore() >= 7;
            case PHOTO -> course.getEmotionalScore() >= 7;
            case HISTORY -> course.getHistoricalScore() >= 7;
            case HIP -> course.getTrendyScore() >= 7;
        };
    }

    private double calculateDistanceToFirstStop(Course course, Double userLat, Double userLon) {
        if (userLat == null || userLon == null || course.getStops().isEmpty()) {
            return Double.MAX_VALUE;
        }

        // 첫 번째 경유지 좌표와 비교
        CourseStop firstStop = course.getStops().stream()
                .filter(s -> s.getStopOrder() == 1)
                .findFirst()
                .orElse(course.getStops().get(0));

        Place place = firstStop.getPlace();
        return calculateDistance(userLat, userLon, place.getLatitude(), place.getLongitude());
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double theta = lon1 - lon2;
        double dist = Math.sin(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2)) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(Math.toRadians(theta));
        dist = Math.acos(dist);
        dist = Math.toDegrees(dist);
        dist = dist * 60 * 1.1515 * 1.609344;
        return dist;
    }

    private record ScoredCourse(Course course, double score, double distance) {
    }

    public CourseResponse getCourseById(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CustomException(ErrorCode.COURSE_NOT_FOUND));

        return fromEntity(course);
    }

    /**
     * 탐방 완료 처리를 수행합니다.
     */
    @Transactional
    public CourseRecordResultResponse completeExploration(Long memberId, CourseCompleteRequest request) {
        log.info("[CourseService] 탐방 완료 처리 시작 - Member: {}, Course: {}", memberId, request.courseId());

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
        Course course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> new CustomException(ErrorCode.COURSE_NOT_FOUND));

        double distance = request.distance();
        int visitedCount = request.visitedSpotIds().size();
        long durationMinutes = Duration.between(request.startTime(), request.endTime()).toMinutes();

        int basePoint = (int) (distance * 10) + (visitedCount * 10);
        int bonusPoint = (member.getCompletedCourseCount() == 0) ? 20 : 0;
        int totalPoint = basePoint + bonusPoint;

        double co2Amount = distance * 0.21;
        double treeEquivalent = co2Amount / 4.7;

        ExploreRecord record = exploreRecordRepository.save(ExploreRecord.builder()
                .member(member)
                .course(course)
                .startTime(request.startTime())
                .endTime(request.endTime())
                .distance(distance)
                .durationMinutes((int) durationMinutes)
                .visitedCount(visitedCount)
                .totalPoint(totalPoint)
                .co2Amount(co2Amount)
                .build());

        member.addStats(totalPoint, distance, co2Amount, visitedCount);

        List<Badge> newBadges = rewardService.checkAndGiveBadges(member);
        Badge lastBadge = newBadges.isEmpty() ? null : newBadges.get(newBadges.size() - 1);

        return new CourseRecordResultResponse(
                record.getId(),
                new CourseRecordResultResponse.Summary(distance, (int) durationMinutes, visitedCount),
                new CourseRecordResultResponse.CO2(co2Amount, treeEquivalent),
                new CourseRecordResultResponse.Reward(basePoint, bonusPoint, totalPoint),
                lastBadge != null ? new CourseRecordResultResponse.BadgeInfo(lastBadge.getCode(), lastBadge.getName())
                        : null);
    }

    public CourseExploreResponse getCourseStopInfo(Long courseId, Integer stopOrder) {
        CourseStop stop = courseStopRepository.findByCourseIdAndStopOrder(courseId, stopOrder)
                .orElseThrow(() -> new CustomException(ErrorCode.STOP_NOT_FOUND));

        Place place = stop.getPlace();
        return new CourseExploreResponse(
                stop.getId(),
                stop.getCode(),
                courseId,
                stop.getCourse().getCode(),
                place.getName(),
                place.getSummary(),
                place.getDescription(),
                place.getLatitude(),
                place.getLongitude(),
                place.getImageUrl());
    }

    private CourseResponse fromEntity(Course course) {
        List<CourseResponse.CourseStop> stopDtos = course.getStops().stream()
                .map(stop -> new CourseResponse.CourseStop(
                        stop.getCode(),
                        stop.getStopOrder(),
                        stop.getPlace().getName(),
                        stop.getPlace().getSummary(),
                        stop.getPlace().getDescription(),
                        stop.getStayMinutes(),
                        stop.getPlace().getLatitude(),
                        stop.getPlace().getLongitude(),
                        stop.getPlace().getImageUrl()))
                .collect(Collectors.toList());

        return new CourseResponse(
                course.getId(),
                course.getCode(),
                course.getTitle(),
                course.getDescription(),
                new CourseResponse.CourseSummary(
                        course.getDistanceKm(),
                        course.getDurationMinutes(),
                        course.getDifficulty(),
                        course.getCarbonReductionKg()),
                stopDtos,
                course.getPolyline(),
                course.getCreatedAt());
    }
}
