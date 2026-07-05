package com.kallavaninc.backend.Services;

import com.kallavaninc.backend.Entities.Role;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class RolePolicyTest {
    @Test
    void allowsFirstTimeNonAdminRole() {
        Assertions.assertDoesNotThrow(() ->
            RolePolicy.validateSelfAssignment(null, Role.Community)
        );
    }

    @Test
    void blocksAdminSelfAssignment() {
        IllegalArgumentException ex = Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> RolePolicy.validateSelfAssignment(null, Role.Admin)
        );
        Assertions.assertTrue(ex.getMessage().contains("Admin"));
    }

    @Test
    void blocksSecondAssignment() {
        IllegalStateException ex = Assertions.assertThrows(
            IllegalStateException.class,
            () -> RolePolicy.validateSelfAssignment(Role.Responder, Role.Community)
        );
        Assertions.assertTrue(ex.getMessage().contains("already"));
    }
}

