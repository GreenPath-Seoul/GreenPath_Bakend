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
import com.seoul.greenpath.domain.course.repository.PlaceRepository;
import com.seoul.greenpath.domain.member.entity.Badge;
import com.seoul.greenpath.domain.member.entity.Level;
import com.seoul.greenpath.domain.member.entity.Member;
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
    private final PlaceRepository placeRepository;
    private final CourseStopRepository courseStopRepository;
    private final ExploreRecordRepository exploreRecordRepository;
    private final MemberRepository memberRepository;
    private final RewardService rewardService;

    /**
     * AI 분석 기반 코스를 추천하고 이를 데이터베이스에 저장합니다.
     */
    @Transactional
    public List<CourseResponse> getRecommendCourses() {
         log.info("[CourseService] 추천 코스 생성 및 저장 시작");

        List<CourseResponse> result = new ArrayList<>();

        for (int i = 0; i < 5; i++) {

                Place place1 = getOrCreatePlace("성북동 고택", "조선시대 양반가의 전통 한옥", 37.59, 127.01, "문화재", "https://picsum.photos/600/400");
                Place place2 = getOrCreatePlace("종로 골목 문화재", "숨겨진 보물 같은 작은 사당", 37.57, 126.99, "문화재", "https://picsum.photos/600/400");
                Place place3 = getOrCreatePlace("숨은 한옥 카페", "전통과 현대가 공존하는 휴식 공간", 37.58, 127.00, "카페", "https://picsum.photos/600/400");

                Course course = Course.builder()
                        .title("조용한 한옥 골목 힐링 라이딩 " + (i + 1))
                        .description("AI 추천 코스 " + (i + 1))
                        .distanceKm(4.2 + i) // 약간씩 다르게
                        .durationMinutes(90)
                        .difficulty(Level.EASY)
                        .carbonReductionKg(1.2)
                        .polyline("encoded_polyline_string")
                        .build();

                course.addStop(CourseStop.builder().place(place1).stopOrder(1).stayMinutes(20).build());
                course.addStop(CourseStop.builder().place(place2).stopOrder(2).stayMinutes(15).build());
                course.addStop(CourseStop.builder().place(place3).stopOrder(3).stayMinutes(30).build());

                Course savedCourse = courseRepository.save(course);

                result.add(fromEntity(savedCourse));
        }

                return result;
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
                lastBadge != null ? new CourseRecordResultResponse.BadgeInfo(lastBadge.getCode(), lastBadge.getName()) : null
        );
    }

    public CourseExploreResponse getCourseStopInfo(Long courseId, Integer stopOrder) {
        CourseStop stop = courseStopRepository.findByCourseIdAndStopOrder(courseId, stopOrder)
                .orElseThrow(() -> new CustomException(ErrorCode.STOP_NOT_FOUND));

        Place place = stop.getPlace();
        return new CourseExploreResponse(
                stop.getId(),
                courseId,
                place.getName(),
                place.getDescription(),
                place.getLatitude(),
                place.getLongitude(),
                place.getImageUrl()
        );
    }

    private Place getOrCreatePlace(String name, String description, Double lat, Double lon, String category, String imageUrl) {
        return placeRepository.findByName(name)
                .orElseGet(() -> placeRepository.save(
                        Place.builder()
                                .name(name)
                                .description(description)
                                .latitude(lat)
                                .longitude(lon)
                                .category(category)
                                .imageUrl(imageUrl)
                                .build()
                ));
    }

    private CourseResponse fromEntity(Course course) {
        List<CourseResponse.CourseStop> stopDtos = course.getStops().stream()
                .map(stop -> new CourseResponse.CourseStop(
                        stop.getStopOrder(),
                        stop.getPlace().getName(),
                        stop.getPlace().getDescription(),
                        stop.getStayMinutes(),
                        stop.getPlace().getLatitude(),
                        stop.getPlace().getLongitude(),
                        stop.getPlace().getImageUrl()
                ))
                .collect(Collectors.toList());

        return new CourseResponse(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                new CourseResponse.CourseSummary(
                        course.getDistanceKm(),
                        course.getDurationMinutes(),
                        course.getDifficulty(),
                        course.getCarbonReductionKg()
                ),
                stopDtos,
                course.getPolyline(),
                course.getCreatedAt()
        );
    }
}
