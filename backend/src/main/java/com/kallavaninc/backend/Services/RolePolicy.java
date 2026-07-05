package com.kallavaninc.backend.Services;

import com.kallavaninc.backend.Entities.Role;

public class RolePolicy {
    private RolePolicy() {
    }

    public static void validateSelfAssignment(Role currentRole, Role requestedRole) {
        if (currentRole != null) {
            throw new IllegalStateException("Role is already assigned");
        }
        if (requestedRole == Role.Admin) {
            throw new IllegalArgumentException("Admin role cannot be self-assigned");
        }
    }
}

