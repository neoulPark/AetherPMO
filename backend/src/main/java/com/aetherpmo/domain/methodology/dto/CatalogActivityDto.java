package com.aetherpmo.domain.methodology.dto;

import java.util.List;

public record CatalogActivityDto(
        String activityCode,
        String activityName,
        List<CatalogTaskDto> tasks
) {
}
