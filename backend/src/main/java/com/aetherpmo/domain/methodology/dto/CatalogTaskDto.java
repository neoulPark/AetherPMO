package com.aetherpmo.domain.methodology.dto;

import java.util.List;

public record CatalogTaskDto(
        Long taskTemplateId,
        String taskCode,
        String taskName,
        Boolean isOptional,
        List<CatalogDeliverableDto> deliverables
) {
}
