package com.aetherpmo.domain.workflow.dto;

import java.util.List;

public record WorkflowDto(
        Long workflowId,
        String name,
        String description,
        Boolean isDefault,
        List<WorkflowStatusDto> statuses,
        List<WorkflowTransitionDto> transitions
) {
}
