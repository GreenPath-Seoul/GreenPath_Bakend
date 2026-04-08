package com.seoul.greenpath.domain.weather.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seoul.greenpath.domain.weather.dto.WeatherResponse;
import com.seoul.greenpath.domain.weather.entity.Weather;
import com.seoul.greenpath.domain.weather.repository.WeatherRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherService {

    private final WeatherRepository weatherRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kma.service-key}")
    private String serviceKey;

    /**
     * 현재 날씨 정보 조회 (DB의 최신 데이터 반환)
     */
    public WeatherResponse getCurrentWeather() {
        return weatherRepository.findFirstByOrderByCreatedAtDesc()
                .map(WeatherResponse::from)
                .orElseThrow(() -> new RuntimeException("날씨 데이터가 존재하지 않습니다."));
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        log.info("[WeatherService] 애플리케이션 준비 완료: 날씨 정보 초기화 시작");
        try {
            fetchAndSaveWeather();
        } catch (Exception e) {
            log.error("[WeatherService] 초기 날씨 정보 수집 실패: {}", e.getMessage());
        }
    }

    @Scheduled(cron = "0 5,35 * * * *") // 매시 5분, 35분에 호출
    @Transactional
    public void fetchAndSaveWeather() {
        log.info("[WeatherService] 기상청 공공데이터 API 호출 시작");

        try {
            LocalDateTime now = LocalDateTime.now();
            String baseDate = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

            // 40분 이전이면 이전 시간 데이터 요청
            LocalDateTime targetTime = now.getMinute() < 40 ? now.minusHours(1) : now;
            String baseTime = targetTime.format(DateTimeFormatter.ofPattern("HH00"));

            URI uri = UriComponentsBuilder
                    .fromUriString("http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getUltraSrtNcst")
                    .queryParam("serviceKey", serviceKey)
                    .queryParam("pageNo", 1)
                    .queryParam("numOfRows", 1000)
                    .queryParam("dataType", "JSON")
                    .queryParam("base_date", baseDate)
                    .queryParam("base_time", baseTime)
                    .queryParam("nx", 55)
                    .queryParam("ny", 127)
                    .build()
                    .toUri();

            log.info("[WeatherService] 기상청 공공데이터 API 요청 URI: {}", uri);
            String response = restTemplate.getForObject(uri, String.class);

            if (response == null || response.contains("<returnAuthMsg>")) {
                log.error("[WeatherService] 기상청 공공데이터 API 인증 실패 또는 오류: {}", response);
                return;
            }

            parseAndSave(response, baseDate, baseTime);

            log.info("[WeatherService] 날씨 데이터 저장 완료 (BaseTime: {})", baseTime);
        } catch (Exception e) {
            log.error("[WeatherService] 날씨 정보 수집 실패: {}", e.getMessage());
        }
    }

    private void parseAndSave(String jsonResponse, String baseDate, String baseTime) throws Exception {
        JsonNode root = objectMapper.readTree(jsonResponse);
        JsonNode items = root.path("response").path("body").path("items").path("item");

        if (items.isMissingNode() || !items.isArray()) {
            log.warn("[WeatherService] 응답 데이터 형식이 올바르지 않거나 데이터가 없습니다.");
            return;
        }

        Weather.WeatherBuilder builder = Weather.builder()
                .baseDate(baseDate)
                .baseTime(baseTime);

        for (JsonNode item : items) {
            String category = item.path("category").asText();
            String obsrValue = item.path("obsrValue").asText();
            log.info("category={}, value={}", category, obsrValue);
            switch (category) {
                case "T1H":
                    builder.temp(obsrValue);
                    break;
                case "PTY":
                    builder.rainType(obsrValue);
                    break;
                case "RN1":
                    builder.rainAmount(obsrValue);
                    break;
                case "REH":
                    builder.humidity(obsrValue);
                    break;
            }
        }

        weatherRepository.deleteAll();
        weatherRepository.save(builder.build());
    }
}
