package com.aetherpmo.auth;

import com.aetherpmo.adapter.amaranth.dto.UserInfo;
import com.aetherpmo.adapter.amaranth.mock.MockUserAdapter;
import com.aetherpmo.auth.dto.LoginResponse;
import com.aetherpmo.common.ApiError;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MockTokenService {

    private static final String MOCK_PASSWORD = "password";

    private final UserRepository userRepository;

    /**
     * Mock login: any known username with password "password" succeeds.
     * Returns token "mock-token-{userId}" and the user info.
     */
    public LoginResponse login(String username, String password) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> ApiError.unauthorized("Invalid credentials"));

        if (!MOCK_PASSWORD.equals(password)) {
            throw ApiError.unauthorized("Invalid credentials");
        }

        String token = "mock-token-" + user.getId();
        return new LoginResponse(token, toInfo(user));
    }

    private UserInfo toInfo(UserEntity u) {
        return MockUserAdapter.toInfo(u);
    }
}
