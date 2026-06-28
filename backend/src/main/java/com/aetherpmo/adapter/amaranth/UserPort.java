package com.aetherpmo.adapter.amaranth;

import com.aetherpmo.adapter.amaranth.dto.UserInfo;

import java.util.List;
import java.util.Optional;

public interface UserPort {

    UserInfo getUser(Long id);

    Optional<UserInfo> findByUsername(String username);

    List<UserInfo> searchUsers(String keyword);
}
