package com.aetherpmo.adapter.amaranth.dto;

public record FileMeta(
        String fileId,
        String fileName,
        String contentType,
        long size
) {
}
