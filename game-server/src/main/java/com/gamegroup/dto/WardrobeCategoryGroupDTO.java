package com.gamegroup.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WardrobeCategoryGroupDTO {

    private String name;

    private Long count;

    private List<WardrobeItemDTO> items;
}
