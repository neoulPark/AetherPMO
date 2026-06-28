package com.aetherpmo.adapter.amaranth.mock;

import com.aetherpmo.adapter.amaranth.TokenValidator;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class MockTokenValidator implements TokenValidator {

    private static final String PREFIX = "mock-token-";

    @Override
    public Optional<Long> validate(String token) {
        if (token == null || !token.startsWith(PREFIX)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(token.substring(PREFIX.length())));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
