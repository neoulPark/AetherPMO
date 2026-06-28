package com.aetherpmo.domain.deliverable.dto;

import jakarta.validation.constraints.NotBlank;

public record AttachmentUploadRequest(
        @NotBlank String fileName,
        String contentType,
        Long fileSize
) {
}
