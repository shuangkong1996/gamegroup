package com.gamegroup.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.gamegroup.dto.LedgerCalendarDayDTO;
import com.gamegroup.dto.LedgerCalendarResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class LedgerCalendarService {

    private static final String CALENDAR_URL =
            "https://cdn.jsdelivr.net/gh/NateScarlet/holiday-cn@master/%d.json";

    private final WebClient.Builder webClientBuilder;
    private final ConcurrentMap<Integer, List<LedgerCalendarDayDTO>> yearCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<Integer, Long> unavailableUntil = new ConcurrentHashMap<>();

    public LedgerCalendarResponse getYear(int year) {
        if (year < 2000 || year > 2099) {
            return response(year, false, List.of());
        }

        Long retryAt = unavailableUntil.get(year);
        if (retryAt != null && retryAt > System.currentTimeMillis()) {
            return response(year, false, List.of());
        }
        unavailableUntil.remove(year);

        List<LedgerCalendarDayDTO> cached = yearCache.get(year);
        if (cached != null) {
            return response(year, true, cached);
        }

        try {
            List<LedgerCalendarDayDTO> days = fetchYear(year);
            yearCache.putIfAbsent(year, days);
            unavailableUntil.remove(year);
            return response(year, true, yearCache.get(year));
        } catch (Exception e) {
            unavailableUntil.put(year, System.currentTimeMillis() + Duration.ofHours(1).toMillis());
            log.warn("Holiday calendar unavailable for year={}", year, e);
            return response(year, false, List.of());
        }
    }

    private List<LedgerCalendarDayDTO> fetchYear(int year) {
        JsonNode root = webClientBuilder.build()
                .get()
                .uri(CALENDAR_URL.formatted(year))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block(Duration.ofSeconds(5));

        JsonNode entries = root == null ? null : root.path("days");
        if (entries == null || !entries.isArray()) {
            throw new IllegalStateException("Holiday calendar response has no days array");
        }

        List<LedgerCalendarDayDTO> days = new ArrayList<>();
        for (JsonNode entry : entries) {
            JsonNode date = entry.path("date");
            if (!date.isTextual() || !entry.path("isOffDay").isBoolean()) {
                continue;
            }
            days.add(LedgerCalendarDayDTO.builder()
                    .date(date.asText())
                    .name(entry.path("name").asText(""))
                    .offDay(entry.path("isOffDay").asBoolean())
                    .build());
        }
        return List.copyOf(days);
    }

    private LedgerCalendarResponse response(int year, boolean available, List<LedgerCalendarDayDTO> days) {
        return LedgerCalendarResponse.builder()
                .year(year)
                .sourceAvailable(available)
                .days(days)
                .build();
    }
}
