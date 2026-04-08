package com.seoul.greenpath.domain.weather.repository;

import com.seoul.greenpath.domain.weather.entity.Weather;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface WeatherRepository extends JpaRepository<Weather, Long> {
    // 가장 최근에 저장된 날씨 정보를 가져옵니다.
    Optional<Weather> findFirstByOrderByCreatedAtDesc();
}
