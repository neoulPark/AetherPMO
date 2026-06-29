package com.aetherpmo.domain.contact.dto;

import com.aetherpmo.adapter.amaranth.dto.UserInfo;
import com.aetherpmo.domain.contact.ContactPoint;

public record ContactPointDto(
        Long contactId,
        Long projectId,
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

    public static ContactPointDto from(ContactPoint c) {
        return from(c, null);
    }

    /**
     * Builds a DTO, enriching INTERNAL contacts from the resolved {@link UserInfo}
     * (Amaranth/mock). When enrichment yields a value it wins; otherwise the
     * stored row value is used.
     */
    public static ContactPointDto from(ContactPoint c, UserInfo user) {
        String name = c.getName();
        String email = c.getEmail();
        if (user != null) {
            if (user.fullName() != null && !user.fullName().isBlank()) {
                name = user.fullName();
            }
            if (user.email() != null && !user.email().isBlank()) {
                email = user.email();
            }
        }
        return new ContactPointDto(
                c.getId(),
                c.getProjectId(),
                c.getField(),
                c.getContactType(),
                c.getUserId(),
                name,
                c.getCompany(),
                c.getDepartment(),
                c.getTitle(),
                c.getPhone(),
                email,
                c.getNote(),
                c.getSortOrder()
        );
    }
}
