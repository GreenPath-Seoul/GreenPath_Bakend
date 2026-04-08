package com.seoul.greenpath.domain.weather.controller;

import com.seoul.greenpath.domain.weather.dto.WeatherResponse;
import com.seoul.greenpath.domain.weather.service.WeatherService;
import com.seoul.greenpath.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Weather", description = "날씨 정보 API")
@RestController
@RequestMapping("/api/v1/weather")
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherService weatherService;

    @Operation(summary = "현재 날씨 조회", description = "기상청에서 수집한 가장 최근의 날씨 정보를 반환합니다.")
    @GetMapping("/current")
    public ApiResponse<WeatherResponse> getCurrentWeather() {
        WeatherResponse response = weatherService.getCurrentWeather();
        return ApiResponse.success("현재 날씨 정보를 성공적으로 조회했습니다.", response);
    }
}
