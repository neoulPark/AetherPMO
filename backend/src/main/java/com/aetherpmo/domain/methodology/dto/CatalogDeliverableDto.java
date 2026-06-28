package com.aetherpmo.domain.methodology.dto;

public record CatalogDeliverableDto(
        Long deliverableTemplateId,
        Integer seqNo,
        String deliverableName,
        Boolean isOptional
) {
}
