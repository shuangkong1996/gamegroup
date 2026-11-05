package com.gamegroup.config;

import com.gamegroup.entity.StaticResource;
import com.gamegroup.repository.StaticResourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StaticResourceDataInitializer implements CommandLineRunner {

    private static final String GROUP_KEY = "anniversary";

    private final StaticResourceRepository staticResourceRepository;

    @Override
    @Transactional
    public void run(String... args) {
        List<SeedResource> resources = List.of(
                new SeedResource(
                        "cover",
                        "纪念册封面",
                        "https://gamegroup.shuangkong.top/oss/game-images/94cab691-5968-413b-b7ee-61584e779fd4.jpg",
                        1
                ),
                new SeedResource(
                        "portrait_groom",
                        "新郎单人照",
                        "https://gamegroup.shuangkong.top/oss/game-images/160bbc40-af03-4a49-93fa-a0196a3c61ba.jpg",
                        2
                ),
                new SeedResource(
                        "portrait_bride",
                        "新娘单人照",
                        "https://gamegroup.shuangkong.top/oss/game-images/9e82906d-7d81-4c21-8510-c1fa6bcc7a20.jpg",
                        3
                ),
                new SeedResource(
                        "moment_1",
                        "纪念照片一",
                        "https://gamegroup.shuangkong.top/oss/game-images/f2f64936-24cf-4c44-94fe-b824277e1cd8.jpg",
                        4
                ),
                new SeedResource(
                        "moment_2",
                        "纪念照片二",
                        "https://gamegroup.shuangkong.top/oss/game-images/791ac7d4-90bf-400e-98c0-40e66d3cb5e7.jpg",
                        5
                ),
                new SeedResource(
                        "moment_3",
                        "纪念照片三",
                        "https://gamegroup.shuangkong.top/oss/game-images/c7a2b08e-543e-4677-80a1-a37734f843d3.jpg",
                        6
                ),
                new SeedResource(
                        "moment_4",
                        "纪念照片四",
                        "https://gamegroup.shuangkong.top/oss/game-images/9ad12286-a119-4bc0-a8f7-f2a8d08dea57.jpg",
                        7
                )
        );

        resources.forEach(this::insertIfMissing);
    }

    private void insertIfMissing(SeedResource seed) {
        if (staticResourceRepository.existsByGroupKeyAndResourceKey(GROUP_KEY, seed.resourceKey())) {
            return;
        }

        StaticResource resource = new StaticResource();
        resource.setGroupKey(GROUP_KEY);
        resource.setResourceKey(seed.resourceKey());
        resource.setResourceName(seed.resourceName());
        resource.setResourceUrl(seed.resourceUrl());
        resource.setSortOrder(seed.sortOrder());
        resource.setStatus(true);
        staticResourceRepository.save(resource);
        log.info("Init static resource: group={}, key={}", GROUP_KEY, seed.resourceKey());
    }

    private record SeedResource(String resourceKey, String resourceName, String resourceUrl, int sortOrder) {}
}
