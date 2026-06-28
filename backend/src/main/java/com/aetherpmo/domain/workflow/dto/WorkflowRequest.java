package com.aetherpmo.domain.workflow.dto;

public record WorkflowRequest(
        String name,
        String description,
        Boolean isDefault
) {
}
