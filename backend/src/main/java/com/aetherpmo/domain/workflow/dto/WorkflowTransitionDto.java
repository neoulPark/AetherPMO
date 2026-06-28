package com.aetherpmo.domain.workflow.dto;

public record WorkflowTransitionDto(
        Long transitionId,
        Long fromStatusId,
        Long toStatusId,
        String name
) {
}
