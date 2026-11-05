package com.gamegroup.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class LedgerCalendarResponse {

    private int year;
    private boolean sourceAvailable;
    private List<LedgerCalendarDayDTO> days;
}
