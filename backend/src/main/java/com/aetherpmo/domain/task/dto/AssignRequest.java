package com.aetherpmo.domain.task.dto;

import jakarta.validation.constraints.NotNull;

public record AssignRequest(
        @NotNull Long assigneeId,
        String reason
) {
}
