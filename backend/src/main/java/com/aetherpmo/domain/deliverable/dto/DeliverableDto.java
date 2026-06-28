package com.aetherpmo.domain.deliverable.dto;

import com.aetherpmo.domain.deliverable.Deliverable;

import java.time.LocalDateTime;

public record DeliverableDto(
        Long id,
        Long projectId,
        Long taskId,
        String deliverableName,
        String deliverableType,
        String status,
        String versionNo,
        Long submittedBy,
        LocalDateTime submittedAt,
        LocalDateTime reviewedAt,
        LocalDateTime approvedAt,
        String authorName,
        long attachmentCount,
        LocalDateTime updatedAt
) {
    public static DeliverableDto from(Deliverable d, String authorName, long attachmentCount) {
        return new DeliverableDto(
                d.getId(),
                d.getProjectId(),
                d.getTaskId(),
                d.getDeliverableName(),
                d.getDeliverableType(),
                d.getStatus(),
                d.getVersionNo(),
                d.getSubmittedBy(),
                d.getSubmittedAt(),
                d.getReviewedAt(),
                d.getApprovedAt(),
                authorName,
                attachmentCount,
                d.getUpdatedAt()
        );
    }
}
