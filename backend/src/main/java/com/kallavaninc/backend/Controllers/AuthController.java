package com.kallavaninc.backend.Controllers;

import com.kallavaninc.backend.Entities.User;
import com.kallavaninc.backend.Services.GoogleTokenVerifier;
import com.kallavaninc.backend.Services.GoogleTokenVerifier.GoogleTokenInfo;
import com.kallavaninc.backend.Services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final GoogleTokenVerifier tokenVerifier;
    private final UserService userService;

    public AuthController(GoogleTokenVerifier tokenVerifier, UserService userService) {
        this.tokenVerifier = tokenVerifier;
        this.userService = userService;
    }

    // --- 1. NEW ENDPOINT TO CHECK FOR EXISTING ADMIN ---
    @GetMapping("/has-admin")
    public ResponseEntity<?> checkHasAdmin() {
        boolean hasAdmin = userService.hasAdmin();
        return ResponseEntity.ok(java.util.Map.of("hasAdmin", hasAdmin));
    }

    @PostMapping("/google")
    public UserResponse loginWithGoogle(@RequestBody GoogleLoginRequest request) {
        GoogleTokenInfo tokenInfo = tokenVerifier.verify(request.idToken());

        // --- 2. EXTRACT FLAG AND PASS BOTH ARGUMENTS ---
        boolean initAdmin = request.initAdmin() != null && request.initAdmin();
        User user = userService.upsertFromGoogle(tokenInfo, initAdmin);

        return UserResponse.from(user);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(ex.getMessage()));
    }

    // --- 3. UPDATED REQUEST TO INCLUDE BOOLEAN FLAG ---
    public record GoogleLoginRequest(String idToken, Boolean initAdmin) {
    }

    public record UserResponse(
            String id,
            String email,
            boolean emailVerified,
            String name,
            String givenName,
            String familyName,
            String role
    ) {
        public static UserResponse from(User user) {
            return new UserResponse(
                    user.getId(),
                    user.getEmail(),
                    user.isEmailVerified(),
                    user.getName(),
                    user.getGivenName(),
                    user.getFamilyName(),
                    user.getRole() != null ? user.getRole().name() : null
            );
        }
    }

    public record ApiError(String message) {
    }
}
