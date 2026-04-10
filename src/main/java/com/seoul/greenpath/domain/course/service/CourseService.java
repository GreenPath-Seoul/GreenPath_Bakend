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
import com.seoul.greenpath.global.openai.OpenAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
        private final OpenAiService openAiService;

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
                if (allCourses.isEmpty())
                        return new ArrayList<>();

                // 탐방 완료된 코스 ID 목록 조회 및 필터링
                Set<Long> completedIds = exploreRecordRepository.findCompletedCourseIdsByMemberId(memberId);
                log.info("[CourseService] 제외할 탐방 완료 코스 수 - Count: {}", completedIds.size());

                List<Course> availableCourses = allCourses.stream()
                                .filter(course -> !completedIds.contains(course.getId()))
                                .collect(Collectors.toList());

                if (availableCourses.isEmpty()) {
                        log.warn("[CourseService] 모든 코스를 탐방하여 새 추천이 불가능함. 기존 전체 코스 사용");
                        availableCourses = allCourses; // 모든 코스를 마친 경우 전체에서 다시 추천
                }

                // 1. 점수 계산 및 거리 계산
                List<ScoredCourse> scoredCourses = availableCourses.stream()
                                .map(course -> {
                                        double score = calculateScore(course, request, preference);
                                        double distance = calculateDistanceToFirstStop(course,
                                                        (request != null && request.latitude() != null)
                                                                        ? request.latitude()
                                                                        : (preference != null ? preference.getLatitude()
                                                                                        : null),
                                                        (request != null && request.longitude() != null)
                                                                        ? request.longitude()
                                                                        : (preference != null
                                                                                        ? preference.getLongitude()
                                                                                        : null));
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
                if (memberId == null || request == null)
                        return null;

                Member member = memberRepository.findById(memberId)
                                .orElse(null);
                if (member == null)
                        return null;

                MemberPreference preference = memberPreferenceRepository.findByMemberId(memberId)
                                .orElseGet(() -> MemberPreference.builder().member(member).build());

                String preferenceText = request.preferenceText();
                float[] embedding = null;
                if (preferenceText != null && !preferenceText.trim().isEmpty()) {
                        // 텍스트가 변경된 경우에만 임베딩 생성 (또는 항상 생성)
                        if (!preferenceText.equals(preference.getPreferenceText())) {
                                log.info("[CourseService] 사용자 취향 임베딩 생성 시작 - Member: {}", memberId);
                                embedding = openAiService.getEmbedding(preferenceText);
                        }
                }

                preference.update(
                                request.mood(),
                                request.duration(),
                                request.level(),
                                request.location(),
                                request.latitude(),
                                request.longitude(),
                                preferenceText,
                                embedding);

                return memberPreferenceRepository.save(preference);
        }

        private double calculateScore(Course course, CourseRequest request, MemberPreference preference) {
                double score = 0.0;

                // 1. 분위기 (Mood) 가중치 (기존 가중치 사용)
                com.seoul.greenpath.domain.member.entity.Mood targetMood = (request != null && request.mood() != null)
                                ? request.mood()
                                : (preference != null ? preference.getMood() : null);

                if (targetMood != null) {
                        score += switch (targetMood) {
                                case QUIET -> course.getHealingScore();
                                case PHOTO -> course.getEmotionalScore();
                                case HISTORY -> course.getHistoricalScore();
                                case HIP -> course.getTrendyScore();
                        };
                }

                // 2. 소요 시간 (Duration) 매칭 (가중치 10점)
                com.seoul.greenpath.domain.member.entity.Duration targetDuration = (request != null
                                && request.duration() != null) ? request.duration()
                                                : (preference != null ? preference.getDuration() : null);

                if (targetDuration != null) {
                        int minutes = course.getDurationMinutes();
                        boolean match = switch (targetDuration) {
                                case ONE_HOUR -> minutes <= 60;
                                case TWO_HOURS -> minutes > 60 && minutes <= 120;
                                case HALF_DAY -> minutes > 120;
                        };
                        if (match)
                                score += 10.0;
                }

                // 3. 난이도 (Level) 매칭 (가중치 5점)
                com.seoul.greenpath.domain.member.entity.Level targetLevel = (request != null
                                && request.level() != null) ? request.level()
                                                : (preference != null ? preference.getLevel() : null);

                if (targetLevel != null) {
                        if (targetLevel == course.getDifficulty() || course
                                        .getDifficulty() == com.seoul.greenpath.domain.member.entity.Level.ANY) {
                                score += 5.0;
                        }
                }

                // 4. 평소 선호 스타일 보너스
                if (preference != null && preference.getMood() != null) {
                        if (matchesMood(course, preference.getMood()))
                                score += 3.0;
                }

                // 5. 🔥 임베딩 유사도 가중치 (추가)
                // 현재 요청의 preferenceText에 대한 임베딩이 있거나, 저장된 preference의 임베딩이 있는 경우
                float[] userEmbedding = (preference != null) ? preference.getEmbedding() : null;
                float[] courseEmbedding = course.getEmbedding();

                if (userEmbedding != null && courseEmbedding != null) {
                        double similarity = cosineSimilarity(userEmbedding, courseEmbedding);
                        // 유사도는 -1 ~ 1 사이이나 대부분 0 ~ 1 사이로 나타남. 가중치 15점 부여.
                        score += similarity * 15.0;
                        log.debug("[CourseService] 코스: {}, 유사도: {}, 추가 점수: {}", course.getTitle(), similarity, similarity * 15.0);
                }

                return score;
        }

        private double cosineSimilarity(float[] vectorA, float[] vectorB) {
                if (vectorA == null || vectorB == null || vectorA.length != vectorB.length) {
                        return 0.0;
                }
                double dotProduct = 0.0;
                double normA = 0.0;
                double normB = 0.0;
                for (int i = 0; i < vectorA.length; i++) {
                        dotProduct += vectorA[i] * vectorB[i];
                        normA += Math.pow(vectorA[i], 2);
                        normB += Math.pow(vectorB[i], 2);
                }
                if (normA == 0 || normB == 0) return 0.0;
                return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
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
                                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                                                * Math.cos(Math.toRadians(theta));
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
                long durationMinutes = Duration.between(request.startTime(), request.endTime()).getSeconds();

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
                                course.getTitle(),
                                new CourseRecordResultResponse.Summary(distance, (int) durationMinutes, visitedCount),
                                new CourseRecordResultResponse.CO2(co2Amount, treeEquivalent),
                                new CourseRecordResultResponse.Reward(basePoint, bonusPoint, totalPoint),
                                lastBadge != null
                                                ? new CourseRecordResultResponse.BadgeInfo(lastBadge.getCode(),
                                                                lastBadge.getName())
                                                : null);
        }

        /**
         * 특정 탐방 기록의 상세 결과를 조회합니다.
         */
        public CourseRecordResultResponse getExploreRecordResult(Long memberId, Long recordId) {
                log.info("[CourseService] 탐방 기록 상세 조회 시작 - Member: {}, Record: {}", memberId, recordId);

                ExploreRecord record = exploreRecordRepository.findById(recordId)
                                .orElseThrow(() -> new CustomException(ErrorCode.RECORD_NOT_FOUND));

                // 본인의 기록인지 확인
                if (!record.getMember().getId().equals(memberId)) {
                        throw new CustomException(ErrorCode.FORBIDDEN);
                }

                // 원본 데이터를 바탕으로 DTO 구성
                double distance = record.getDistance();
                int visitedCount = record.getVisitedCount();
                int durationMinutes = record.getDurationMinutes();
                double co2Amount = record.getCo2Amount();
                double treeEquivalent = co2Amount / 4.7;

                // 포인트 역계산 (필요 시) - 현재는 단순 계산 방식
                int basePoint = (int) (distance * 10) + (visitedCount * 10);
                int totalPoint = record.getTotalPoint();
                int bonusPoint = totalPoint - basePoint;

                return new CourseRecordResultResponse(
                                record.getId(),
                                record.getCourse().getTitle(),
                                new CourseRecordResultResponse.Summary(distance, durationMinutes, visitedCount),
                                new CourseRecordResultResponse.CO2(co2Amount, treeEquivalent),
                                new CourseRecordResultResponse.Reward(basePoint, bonusPoint, totalPoint),
                                null // 과거 기록 조회 시 배지 획득 여부는 일단 null (이미 획득했을 것이므로)
                );
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
                                place.getImageUrl(),
                                stop.getDistanceFromPrev(),
                                stop.getDurationFromPrev());
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
                                                stop.getPlace().getImageUrl(),
                                                stop.getDistanceFromPrev(),
                                                stop.getDurationFromPrev()))
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
    /**
     * C0001 ~ C0010 중 랜덤으로 3개의 코스를 반환합니다.
     */
    public List<CourseResponse> getRandomCourses() {
        log.info("[CourseService] 랜덤 코스 조회 요청 (C0001~C0010)");
        List<String> codes = java.util.stream.IntStream.rangeClosed(1, 10)
                .mapToObj(i -> String.format("C%04d", i))
                .collect(Collectors.toList());

        List<Course> courses = courseRepository.findAllByCodeIn(codes);
        java.util.Collections.shuffle(courses);

        return courses.stream()
                .limit(3)
                .map(this::fromEntity)
                .collect(Collectors.toList());
    }
}
