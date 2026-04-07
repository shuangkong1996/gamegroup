package com.gamegroup.dto;

import java.util.List;

public class BoxDetailDTO {

    private BoxDTO box;

    private List<BoxItemDTO> items;

    public BoxDTO getBox() {
        return box;
    }

    public void setBox(BoxDTO box) {
        this.box = box;
    }

    public List<BoxItemDTO> getItems() {
        return items;
    }

    public void setItems(List<BoxItemDTO> items) {
        this.items = items;
    }
}
