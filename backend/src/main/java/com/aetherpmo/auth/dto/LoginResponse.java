package com.aetherpmo.auth.dto;

import com.aetherpmo.adapter.amaranth.dto.UserInfo;

public record LoginResponse(
        String token,
        UserInfo user
) {
}
