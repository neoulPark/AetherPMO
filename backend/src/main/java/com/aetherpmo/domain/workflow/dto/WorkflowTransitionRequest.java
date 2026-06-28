package com.aetherpmo.domain.workflow.dto;

public record WorkflowTransitionRequest(
        Long fromStatusId,
        Long toStatusId,
        String name
) {
}
