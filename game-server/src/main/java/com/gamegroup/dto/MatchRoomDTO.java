package com.gamegroup.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchRoomDTO {

    private Long id;

    private Long gameId;

    private String gameName;

    private String verifyCode;

    private List<Long> playerIds;

    private Integer currentPlayers;

    private Integer maxPlayers;

    private String status;

    private LocalDateTime expireTime;
}
