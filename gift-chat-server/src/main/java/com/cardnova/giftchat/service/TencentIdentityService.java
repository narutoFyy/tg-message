package com.cardnova.giftchat.service;

import com.cardnova.giftchat.entity.UserEntity;
import com.cardnova.giftchat.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TencentIdentityService {

    private final UserRepository userRepository;

    public TencentIdentityService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public String ensureTencentUserId(UserEntity user) {
        if (user == null) {
            return "";
        }
        if (user.getTencentUserId() != null && !user.getTencentUserId().isBlank()) {
            return user.getTencentUserId();
        }
        String candidate = "u_" + user.getId().replace("-", "");
        if (candidate.length() > 32) {
            candidate = candidate.substring(0, 32);
        }
        user.setTencentUserId(candidate);
        userRepository.save(user);
        return candidate;
    }
}
