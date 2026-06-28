package com.aetherpmo.domain.methodology.dto;

import java.util.List;

public record CatalogNodeDto(
        Long nodeId,
        Long parentNodeId,
        String nodeType,
        String code,
        String name,
        Boolean isOptional,
        Integer seqNo,
        Integer sortOrder,
        Long workflowId,
        List<CatalogNodeDto> children
) {
}
