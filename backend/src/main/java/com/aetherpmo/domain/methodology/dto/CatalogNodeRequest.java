package com.aetherpmo.domain.methodology.dto;

public record CatalogNodeRequest(
        Long parentNodeId,
        String nodeType,
        String code,
        String name,
        Boolean isOptional,
        Integer sortOrder,
        Integer seqNo,
        String description,
        String deliverableCategory,
        String stage
) {
}
