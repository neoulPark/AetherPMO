package com.aetherpmo.domain.deliverable.dto;

import jakarta.validation.constraints.NotBlank;

public record DeliverableCreateRequest(
        @NotBlank String deliverableName,
        String deliverableType,
        String versionNo,
        Long taskId,
        String status
) {
}
