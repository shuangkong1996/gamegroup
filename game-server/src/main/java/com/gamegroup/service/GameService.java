package com.gamegroup.service;

import com.gamegroup.common.exception.BusinessException;
import com.gamegroup.common.response.ErrorCode;
import com.gamegroup.dto.GameDTO;
import com.gamegroup.entity.Game;
import com.gamegroup.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GameService {

    private final GameRepository gameRepository;

    public List<GameDTO> getActiveGames() {
        return gameRepository.findByStatusTrueOrderBySortOrderAsc()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public GameDTO getGameById(Long id) {
        Game game = gameRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.GAME_NOT_FOUND));

        if (!game.getStatus()) {
            throw new BusinessException(ErrorCode.GAME_DISABLED);
        }

        return toDTO(game);
    }

    private GameDTO toDTO(Game game) {
        return GameDTO.builder()
                .id(game.getId())
                .name(game.getName())
                .icon(game.getIcon())
                .description(game.getDescription())
                .rules(game.getRules())
                .minPlayers(game.getMinPlayers())
                .maxPlayers(game.getMaxPlayers())
                .build();
    }
}
