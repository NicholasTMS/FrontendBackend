package com.kallavaninc.backend.Repositories;

import com.kallavaninc.backend.Entities.AuthProvider;
import com.kallavaninc.backend.Entities.User;
import com.kallavaninc.backend.Entities.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByProviderAndProviderSubject(AuthProvider provider, String providerSubject);

    Optional<User> findByEmail(String email);
    boolean existsByRole(Role role);
}

