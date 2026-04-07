package com.gamegroup.dto.mortgage;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MortgageCalculateResponse {

    private MortgageCombinedSummaryDTO combined;
    private List<MortgageLoanSliceDTO> loans;
    private List<MortgageScheduleRowDTO> combinedSchedule;
    private MortgagePrepayAdviceDTO prepayAdvice;
}
