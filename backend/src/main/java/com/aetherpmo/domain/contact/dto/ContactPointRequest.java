package com.aetherpmo.domain.contact.dto;

public record ContactPointRequest(
        String field,
        String contactType,
        Long userId,
        String name,
        String company,
        String department,
        String title,
        String phone,
        String email,
        String note,
        Integer sortOrder
) {
}
