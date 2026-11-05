package com.gamegroup.service;

import com.gamegroup.dto.StaticResourceDTO;
import com.gamegroup.entity.StaticResource;
import com.gamegroup.repository.StaticResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StaticResourceService {

    private final StaticResourceRepository staticResourceRepository;

    public List<StaticResourceDTO> getByGroup(String groupKey) {
        return staticResourceRepository.findByGroupKeyAndStatusTrueOrderBySortOrderAscIdAsc(groupKey)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    private StaticResourceDTO toDTO(StaticResource resource) {
        return StaticResourceDTO.builder()
                .groupKey(resource.getGroupKey())
                .resourceKey(resource.getResourceKey())
                .resourceName(resource.getResourceName())
                .resourceUrl(resource.getResourceUrl())
                .sortOrder(resource.getSortOrder())
                .build();
    }
}
