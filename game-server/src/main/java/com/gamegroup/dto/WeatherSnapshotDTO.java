package com.gamegroup.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeatherSnapshotDTO {

    private String city;

    private String targetDate;

    private String weatherCondition;

    private Integer temperatureMin;

    private Integer temperatureMax;

    private Integer humidity;

    private String weatherJson;
}
