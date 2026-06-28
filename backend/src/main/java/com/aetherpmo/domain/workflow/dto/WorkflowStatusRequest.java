package com.aetherpmo.domain.workflow.dto;

public record WorkflowStatusRequest(
        String code,
        String name,
        String color,
        String category,
        Boolean isInitial,
        Boolean isFinal,
        Integer sortOrder
) {
}
