package com.aetherpmo.domain.deliverable.dto;

public record DeliverableUpdateRequest(
        String deliverableName,
        String deliverableType,
        String versionNo,
        Long taskId
) {
}
