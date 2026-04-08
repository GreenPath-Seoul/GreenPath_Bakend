package com.seoul.greenpath.domain.weather.dto;

import com.seoul.greenpath.domain.weather.entity.Weather;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WeatherResponse {
    private String temp;
    private String rainType;
    private String rainAmount;
    private String humidity;
    private String baseDate;
    private String baseTime;

    public static WeatherResponse from(Weather weather) {
        return WeatherResponse.builder()
                .temp(weather.getTemp())
                .rainType(weather.getRainType())
                .rainAmount(weather.getRainAmount())
                .humidity(weather.getHumidity())
                .baseDate(weather.getBaseDate())
                .baseTime(weather.getBaseTime())
                .build();
    }
}
