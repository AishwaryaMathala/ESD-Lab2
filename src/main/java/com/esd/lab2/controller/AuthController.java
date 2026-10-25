package com.esd.lab2.controller;

import com.esd.lab2.dto.SignupRequest;
import com.esd.lab2.dto.UserResponse;
import com.esd.lab2.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * S1 — FILL IN MANUALLY (no AI for this slice).
 *
 * This controller handles all /api/auth endpoints.
 * In S1 you add the signup endpoint.
 * In S2 (AI) you will add login and later logout.
 *
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * A1 — POST /api/auth/signup
     * Public endpoint. Returns 201 with UserResponse on success.
     */
    @PostMapping("/signup")
    public ResponseEntity<UserResponse> signup(@Valid @RequestBody SignupRequest request) {
        // TODO: call authService.signup(request) and return 201 with the result
        return null;
    }

    // S2: login endpoint will be added here by AI
    // S5: logout endpoint will be added here by AI
}
