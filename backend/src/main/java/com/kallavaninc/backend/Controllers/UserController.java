package com.kallavaninc.backend.Controllers;

import com.kallavaninc.backend.Entities.Role;
import com.kallavaninc.backend.Entities.User;
import com.kallavaninc.backend.Services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/{userId}/role")
    public AuthController.UserResponse assignRoleOnce(
        @PathVariable String userId,
        @RequestBody RoleRequest request
    ) {
        User user = userService.assignSelfRoleOnce(userId, request.role());
        return AuthController.UserResponse.from(user);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<AuthController.ApiError> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new AuthController.ApiError(ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<AuthController.ApiError> handleConflict(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new AuthController.ApiError(ex.getMessage()));
    }

    public record RoleRequest(Role role) {
    }
}

