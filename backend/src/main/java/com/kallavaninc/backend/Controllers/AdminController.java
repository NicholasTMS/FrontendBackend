package com.kallavaninc.backend.Controllers;

import com.kallavaninc.backend.Entities.Role;
import com.kallavaninc.backend.Entities.User;
import com.kallavaninc.backend.Services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    @PutMapping("/users/{userId}/role")
    public AuthController.UserResponse assignRoleAsAdmin(
        @PathVariable String userId,
        @RequestBody RoleRequest request
    ) {
        // TODO: protect this endpoint with admin-only authorization.
        User user = userService.assignRoleAsAdmin(userId, request.role());
        return AuthController.UserResponse.from(user);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<AuthController.ApiError> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new AuthController.ApiError(ex.getMessage()));
    }

    public record RoleRequest(Role role) {
    }
}

