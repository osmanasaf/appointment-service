package com.asaf.appointment_service.common.service;

import com.asaf.appointment_service.common.exception.UnauthorizedException;
import com.asaf.appointment_service.entity.user.User;
import com.asaf.appointment_service.entity.user.UserRole;
import com.asaf.appointment_service.repository.user.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("Bu endpoint için giriş yapmalısınız");
        }

        String username = authentication.getName();
        if (username == null || username.isBlank() || "anonymousUser".equals(username)) {
            throw new UnauthorizedException("Bu endpoint için giriş yapmalısınız");
        }

        return userRepository.findByEmail(username)
            .orElseThrow(() -> new UnauthorizedException("Kullanıcı bulunamadı"));
    }

    @Transactional(readOnly = true)
    public Long getCurrentUserId() {
        return getCurrentUser().getId();
    }

    @Transactional(readOnly = true)
    public Long getCurrentBusinessId() {
        User user = getCurrentUser();
        if (user.getBusinessId() == null) {
            throw new UnauthorizedException("Kullanıcıya bağlı işletme bulunamadı");
        }
        return user.getBusinessId();
    }

    @Transactional(readOnly = true)
    public UserRole getCurrentUserRole() {
        return getCurrentUser().getRole();
    }

    @Transactional(readOnly = true)
    public Long getCurrentEmployeeId() {
        return getCurrentUser().getEmployeeId();
    }

    @Transactional(readOnly = true)
    public boolean isEmployee() {
        return getCurrentUserRole() == UserRole.EMPLOYEE;
    }

    @Transactional(readOnly = true)
    public boolean isBusinessOwner() {
        return getCurrentUserRole() == UserRole.BUSINESS_OWNER;
    }

    @Transactional(readOnly = true)
    public boolean isAdmin() {
        return getCurrentUserRole() == UserRole.ADMIN;
    }

    @Transactional(readOnly = true)
    public void requireBusinessOwnerOrAdmin() {
        UserRole role = getCurrentUserRole();
        if (role != UserRole.BUSINESS_OWNER && role != UserRole.ADMIN) {
            throw new UnauthorizedException("Bu işlem için işletme sahibi veya admin yetkisi gereklidir");
        }
    }

    @Transactional(readOnly = true)
    public boolean isSuperAdmin() {
        return getCurrentUserRole() == UserRole.SUPER_ADMIN;
    }

    @Transactional(readOnly = true)
    public void requireSuperAdmin() {
        if (getCurrentUserRole() != UserRole.SUPER_ADMIN) {
            throw new UnauthorizedException("Bu işlem için platform yöneticisi yetkisi gereklidir");
        }
    }
}
