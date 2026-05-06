package com.gamegroup.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamegroup.dto.WeatherSnapshotDTO;
import com.gamegroup.entity.WeatherSnapshot;
import com.gamegroup.entity.User;
import com.gamegroup.repository.WeatherSnapshotRepository;
import com.gamegroup.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final WeatherSnapshotRepository weatherSnapshotRepository;
    private final UserRepository userRepository;

    @Transactional
    public WeatherSnapshotDTO getTomorrowWeather(Long userId, String city) {
        LocalDate targetDate = LocalDate.now().plusDays(1);
        String safeCity = getPreferredCity(userId, city);

        return weatherSnapshotRepository.findByUserIdAndCityAndTargetDate(userId, safeCity, targetDate)
                .filter(snapshot -> snapshot.getExpireTime() != null && snapshot.getExpireTime().isAfter(LocalDateTime.now()))
                .map(this::toDto)
                .orElseGet(() -> createDefaultSnapshot(userId, safeCity, targetDate));
    }

    public String getPreferredCity(Long userId, String city) {
        if (city != null && !city.isBlank()) {
            return city.trim();
        }
        return userRepository.findById(userId)
                .map(User::getCity)
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .orElse("杭州");
    }

    private WeatherSnapshotDTO createDefaultSnapshot(Long userId, String city, LocalDate targetDate) {
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("city", city);
        raw.put("targetDate", targetDate.toString());
        raw.put("condition", "晴到多云");
        raw.put("temperatureMin", 20);
        raw.put("temperatureMax", 28);
        raw.put("humidity", 60);

        WeatherSnapshot snapshot = weatherSnapshotRepository.findByUserIdAndCityAndTargetDate(userId, city, targetDate)
                .orElseGet(WeatherSnapshot::new);
        snapshot.setUserId(userId);
        snapshot.setCity(city);
        snapshot.setTargetDate(targetDate);
        snapshot.setWeatherCondition("晴到多云");
        snapshot.setTemperatureMin(20);
        snapshot.setTemperatureMax(28);
        snapshot.setHumidity(60);
        snapshot.setWeatherJson(toJson(raw));
        snapshot.setExpireTime(LocalDateTime.now().plusHours(6));
        snapshot.setUpdateTime(LocalDateTime.now());
        weatherSnapshotRepository.save(snapshot);
        return toDto(snapshot);
    }

    private WeatherSnapshotDTO toDto(WeatherSnapshot snapshot) {
        return WeatherSnapshotDTO.builder()
                .city(snapshot.getCity())
                .targetDate(snapshot.getTargetDate().toString())
                .weatherCondition(snapshot.getWeatherCondition())
                .temperatureMin(snapshot.getTemperatureMin())
                .temperatureMax(snapshot.getTemperatureMax())
                .humidity(snapshot.getHumidity())
                .weatherJson(snapshot.getWeatherJson())
                .build();
    }

    private String toJson(Map<String, Object> data) {
        try {
            return OBJECT_MAPPER.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.warn("天气快照 JSON 序列化失败", e);
            return "{}";
        }
    }
}
