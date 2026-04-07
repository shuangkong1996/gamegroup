package com.gamegroup.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class MakeChoiceRequest {

    @NotNull(message = "roomId不能为空")
    private Long roomId;

    @NotNull(message = "choice不能为空")
    @Pattern(regexp = "^(ROCK|PAPER|SCISSORS)$", message = "choice必须是ROCK/PAPER/SCISSORS")
    private String choice;
}
