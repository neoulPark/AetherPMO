package com.aetherpmo.domain.user;

import com.aetherpmo.adapter.amaranth.UserPort;
import com.aetherpmo.adapter.amaranth.dto.UserInfo;
import com.aetherpmo.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Internal-person picker for INTERNAL contact points. Backed by {@link UserPort#searchUsers}
 * (MockUserAdapter serves seeded pms_user). Empty q returns all matches.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UserController {

    private final UserPort userPort;

    @GetMapping("/users/search")
    public ApiResponse<List<UserInfo>> search(@RequestParam(value = "q", required = false) String q) {
        return ApiResponse.ok(userPort.searchUsers(q == null ? "" : q));
    }
}
