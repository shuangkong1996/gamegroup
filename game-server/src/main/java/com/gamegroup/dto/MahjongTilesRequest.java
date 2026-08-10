package com.gamegroup.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class MahjongTilesRequest {

    @NotEmpty(message = "手牌不能为空")
    private List<String> tiles;

    public List<String> getTiles() {
        return tiles;
    }

    public void setTiles(List<String> tiles) {
        this.tiles = tiles;
    }
}
