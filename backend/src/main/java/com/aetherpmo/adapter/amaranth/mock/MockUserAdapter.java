package com.aetherpmo.adapter.amaranth.mock;

import com.aetherpmo.adapter.amaranth.UserPort;
import com.aetherpmo.adapter.amaranth.dto.UserInfo;
import com.aetherpmo.auth.UserEntity;
import com.aetherpmo.auth.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MockUserAdapter implements UserPort {

    private final UserRepository userRepository;

    @Override
    public UserInfo getUser(Long id) {
        return userRepository.findById(id)
                .map(MockUserAdapter::toInfo)
                .orElse(null);
    }

    @Override
    public Optional<UserInfo> findByUsername(String username) {
        return userRepository.findByUsername(username).map(MockUserAdapter::toInfo);
    }

    @Override
    public List<UserInfo> searchUsers(String keyword) {
        String kw = keyword == null ? "" : keyword;
        return userRepository
                .findByUsernameContainingIgnoreCaseOrFullNameContainingIgnoreCase(kw, kw)
                .stream()
                .map(MockUserAdapter::toInfo)
                .toList();
    }

    public static UserInfo toInfo(UserEntity u) {
        return new UserInfo(
                u.getId(),
                u.getUsername(),
                u.getEmail(),
                u.getFullName(),
                u.getRole(),
                Boolean.TRUE.equals(u.getActive())
        );
    }
}
