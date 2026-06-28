package com.aetherpmo.domain.deliverable.dto;

import com.aetherpmo.domain.deliverable.Attachment;

import java.time.LocalDateTime;

public record AttachmentDto(
        Long id,
        String entityType,
        Long entityId,
        String fileRef,
        String fileName,
        Long fileSize,
        String contentType,
        Integer sortOrder,
        String uploadedBy,
        LocalDateTime uploadedAt
) {
    public static AttachmentDto from(Attachment a) {
        return new AttachmentDto(
                a.getId(),
                a.getEntityType(),
                a.getEntityId(),
                a.getFileRef(),
                a.getFileName(),
                a.getFileSize(),
                a.getContentType(),
                a.getSortOrder(),
                a.getUploadedBy(),
                a.getUploadedAt()
        );
    }
}
