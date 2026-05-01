package com.seoul.greenpath.domain.weather.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeatherApiClient {

    private final RestTemplate restTemplate;

    @CircuitBreaker(name = "weatherApi", fallbackMethod = "fetchWeatherFallback")
    public String fetchWeather(URI uri) {
        return restTemplate.getForObject(uri, String.class);
    }

    public String fetchWeatherFallback(URI uri, Throwable t) {
        log.error("[WeatherApiClient] 기상청 API 호출 실패 또는 차단됨 (Cause: {})", t.getMessage());
        return "FALLBACK";
    }
}
