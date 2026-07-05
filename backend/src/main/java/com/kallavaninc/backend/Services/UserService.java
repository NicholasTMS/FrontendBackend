package com.kallavaninc.backend.Services;

import com.kallavaninc.backend.Entities.AuthProvider;
import com.kallavaninc.backend.Entities.Role;
import com.kallavaninc.backend.Entities.User;
import com.kallavaninc.backend.Repositories.UserRepository;
import com.kallavaninc.backend.Services.GoogleTokenVerifier.GoogleTokenInfo;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // --- 1. NEW METHOD TO CHECK FOR ADMIN ---
    public boolean hasAdmin() {
        return userRepository.existsByRole(Role.Admin);
    }

    // --- 2. UPDATED METHOD SIGNATURE (added boolean initAdmin) ---
    public User upsertFromGoogle(GoogleTokenInfo tokenInfo, boolean initAdmin) {
        User user = userRepository
                .findByProviderAndProviderSubject(AuthProvider.Google, tokenInfo.sub())
                .orElseGet(User::new);

        user.setProvider(AuthProvider.Google);
        user.setProviderSubject(tokenInfo.sub());
        user.setEmail(tokenInfo.email());
        user.setEmailVerified(tokenInfo.emailVerified());
        user.setName(tokenInfo.name() != null ? tokenInfo.name() : tokenInfo.email());
        user.setGivenName(tokenInfo.givenName());
        user.setFamilyName(tokenInfo.familyName());
        user.setLastLoginAt(Instant.now());

        // BOOTSTRAP PATTERN: Automatically make them admin if requested and no admin exists yet
        if (initAdmin && !hasAdmin()) {
            user.setRole(Role.Admin);
            user.setRoleAssignedAt(Instant.now());
        }

        return userRepository.save(user);
    }

    public User assignSelfRoleOnce(String userId, Role role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        RolePolicy.validateSelfAssignment(user.getRole(), role);

        user.setRole(role);
        user.setRoleAssignedAt(Instant.now());
        return userRepository.save(user);
    }

    public User assignRoleAsAdmin(String userId, Role role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setRole(role);
        user.setRoleAssignedAt(Instant.now());
        return userRepository.save(user);
    }

    // Add this to UserService
    public java.util.List<User> getAllUsers() {
        return userRepository.findAll();
    }
}

