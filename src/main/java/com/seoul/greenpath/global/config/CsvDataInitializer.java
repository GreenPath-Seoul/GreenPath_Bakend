package com.seoul.greenpath.global.config;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.seoul.greenpath.domain.course.entity.Course;
import com.seoul.greenpath.domain.course.entity.CourseStop;
import com.seoul.greenpath.domain.course.entity.Place;
import com.seoul.greenpath.domain.course.repository.CourseRepository;
import com.seoul.greenpath.domain.course.repository.CourseStopRepository;
import com.seoul.greenpath.domain.course.repository.PlaceRepository;
import com.seoul.greenpath.domain.member.entity.Level;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(2) // DataInitializer(뱃지) 다음으로 실행
public class CsvDataInitializer implements CommandLineRunner {

    private final PlaceRepository placeRepository;
    private final CourseRepository courseRepository;
    private final CourseStopRepository courseStopRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (placeRepository.count() > 0) {
            log.info("Data already exists. Skipping CSV import.");
            return;
        }

        importPlaces();
        importCourses();
        importCourseStops();
        
        log.info("CSV data import completed successfully.");
    }

    private void importPlaces() throws Exception {
        CsvMapper mapper = new CsvMapper();
        CsvSchema schema = CsvSchema.emptySchema().withHeader();
        
        ClassPathResource resource = new ClassPathResource("PlaceDb.csv");
        try (InputStream is = resource.getInputStream()) {
            MappingIterator<Map<String, String>> it = mapper.readerFor(Map.class)
                    .with(schema)
                    .readValues(is);

            while (it.hasNext()) {
                Map<String, String> row = it.next();
                Place place = Place.builder()
                        .code(row.get("code"))
                        .name(row.get("name"))
                        .address(row.get("address"))
                        .latitude(parseSafeDouble(row.get("latitude")))
                        .longitude(parseSafeDouble(row.get("longitude")))
                        .summary(row.get("summary"))
                        .description(row.get("description"))
                        .closedDays(row.get("휴무일"))
                        .operatingHours(row.get("운영시간"))
                        .imageUrl(row.get("imageUrl"))
                        .category(row.get("category"))
                        .build();
                placeRepository.save(place);
            }
        }
        log.info("Imported Places from CSV.");
    }

    private void importCourses() throws Exception {
        CsvMapper mapper = new CsvMapper();
        CsvSchema schema = CsvSchema.emptySchema().withHeader();
        
        ClassPathResource resource = new ClassPathResource("CourseDb.csv");

        try (InputStream is = resource.getInputStream()) {
            MappingIterator<Map<String, String>> it = mapper.readerFor(Map.class)
                    .with(schema)
                    .readValues(is);

            while (it.hasNext()) {
                Map<String, String> row = it.next();

                // ✅ difficulty 변환
                String difficultyStr = row.get("difficulty");
                Level level = Level.ANY;

                if ("쉬움".equals(difficultyStr)) level = Level.EASY;
                else if ("보통".equals(difficultyStr)) level = Level.MEDIUM;
                else if ("상관없음".equals(difficultyStr)) level = Level.ANY;

                // ✅ 거리
                Double distanceKm = parseSafeDouble(row.get("distanceKm"));

                // ✅ 점수들 (null 안전)
                double healingScore = parseSafeDouble(row.get("healingScore"));
                double emotionalScore = parseSafeDouble(row.get("emotionalScore"));
                double historicalScore = parseSafeDouble(row.get("historicalScore"));
                double trendyScore = parseSafeDouble(row.get("trendyScore"));

                Course course = Course.builder()
                        .code(row.get("code"))
                        .title(row.get("title"))
                        .description(row.get("description"))
                        .distanceKm(distanceKm)
                        .durationMinutes(parseSafeInt(row.get("durationMinutes")))
                        .difficulty(level)
                        .carbonReductionKg(distanceKm != null ? Math.round(distanceKm * 0.2 * 10) / 10.0 : 0.0)
                        // 🔥 핵심 추가
                        .healingScore(healingScore)
                        .emotionalScore(emotionalScore)
                        .historicalScore(historicalScore)
                        .trendyScore(trendyScore)

                        .build();

                courseRepository.save(course);
            }
        }

        log.info("Imported Courses from CSV.");
    }

    private void importCourseStops() throws Exception {
        CsvMapper mapper = new CsvMapper();
        CsvSchema schema = CsvSchema.emptySchema().withHeader();
        
        ClassPathResource resource = new ClassPathResource("CourseStopDb.csv");
        try (InputStream is = resource.getInputStream()) {
            MappingIterator<Map<String, String>> it = mapper.readerFor(Map.class)
                    .with(schema)
                    .readValues(is);

            while (it.hasNext()) {
                Map<String, String> row = it.next();
                
                Course course = courseRepository.findByCode(row.get("CourseCode"))
                        .orElseThrow(() -> new RuntimeException("Course not found: " + row.get("CourseCode")));
                
                Place place = placeRepository.findByCode(row.get("PlaceCode"))
                        .orElseThrow(() -> new RuntimeException("Place not found: " + row.get("PlaceCode")));
                
                CourseStop stop = CourseStop.builder()
                        .course(course)
                        .place(place)
                        .stopOrder(parseSafeInt(row.get("stopOrder")))
                        .stayMinutes(parseSafeInt(row.get("stayMinutes")))
                        .build();
                
                courseStopRepository.save(stop);
            }
        }
        log.info("Imported CourseStops from CSV.");
    }

    private Double parseSafeDouble(String value) {
        if (value == null || value.trim().isEmpty()) return 0.0;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private Integer parseSafeInt(String value) {
        if (value == null || value.trim().isEmpty()) return 0;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
