package com.aetherpmo.domain.task.dto;

import jakarta.validation.constraints.NotBlank;

public record TaskDeliverableCreateRequest(
        @NotBlank String deliverableName,
        String deliverableType
) {
}
