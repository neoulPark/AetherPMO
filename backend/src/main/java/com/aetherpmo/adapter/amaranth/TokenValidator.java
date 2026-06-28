package com.aetherpmo.adapter.amaranth;

import java.util.Optional;

public interface TokenValidator {

    /**
     * Validates the token and returns the user id it refers to, if valid.
     */
    Optional<Long> validate(String token);
}
