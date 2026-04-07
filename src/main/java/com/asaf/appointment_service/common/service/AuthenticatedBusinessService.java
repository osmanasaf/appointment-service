package com.asaf.appointment_service.common.service;

import com.asaf.appointment_service.common.exception.UnauthorizedException;
import com.asaf.appointment_service.repository.user.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticatedBusinessService {

    private final UserRepository userRepository;

    public AuthenticatedBusinessService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Long getCurrentBusinessId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("Bu endpoint için giriş yapmalısınız");
        }

        String username = authentication.getName();
        if (username == null || username.isBlank() || "anonymousUser".equals(username)) {
            throw new UnauthorizedException("Bu endpoint için giriş yapmalısınız");
        }

        return userRepository.findByEmail(username)
            .map(user -> {
                if (user.getBusinessId() == null) {
                    throw new UnauthorizedException("Kullanıcıya bağlı işletme bulunamadı");
                }
                return user.getBusinessId();
            })
            .orElseThrow(() -> new UnauthorizedException("Kullanıcı bulunamadı"));
    }
}
