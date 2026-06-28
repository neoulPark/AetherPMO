package com.aetherpmo.auth;

import com.aetherpmo.adapter.amaranth.UserPort;
import com.aetherpmo.adapter.amaranth.dto.UserInfo;
import com.aetherpmo.auth.dto.LoginRequest;
import com.aetherpmo.auth.dto.LoginResponse;
import com.aetherpmo.common.ApiError;
import com.aetherpmo.common.ApiResponse;
import com.aetherpmo.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final MockTokenService tokenService;
    private final UserPort userPort;
    private final CurrentUser currentUser;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(tokenService.login(request.username(), request.password()));
    }

    @GetMapping("/me")
    public ApiResponse<UserInfo> me() {
        UserInfo user = userPort.getUser(currentUser.id());
        if (user == null) {
            throw ApiError.notFound("User not found");
        }
        return ApiResponse.ok(user);
    }
}
