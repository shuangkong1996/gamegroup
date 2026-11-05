package com.gamegroup.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LedgerCalendarDayDTO {

    private String date;
    private String name;
    private boolean offDay;
}
