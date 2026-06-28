package com.aetherpmo.adapter.amaranth.dto;

public record UserInfo(
        Long id,
        String username,
        String email,
        String fullName,
        String role,
        boolean active
) {
}
