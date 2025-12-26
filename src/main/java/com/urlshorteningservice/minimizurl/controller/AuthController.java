package com.urlshorteningservice.minimizurl.controller;

import com.urlshorteningservice.minimizurl.dto.RegisterRequest;
import com.urlshorteningservice.minimizurl.dto.ResetPasswordRequest;
import com.urlshorteningservice.minimizurl.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest request) {
        // Pass the DTO instead of individual strings
        authService.register(request);
        return ResponseEntity.ok("User registered successfully!");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody RegisterRequest request) {
        String token = authService.login(request.getUsername(), request.getPassword());
        return ResponseEntity.ok(Map.of("token", token));
    }

    @GetMapping("/auth-success")
    public ResponseEntity<Map<String, String>> authSuccess(@RequestParam String token) {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Google Authentication Successful!");
        response.put("token", token);
        response.put("type", "Bearer");
        return ResponseEntity.ok(response);
    }
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestParam String email) {
        try {
            authService.processForgotPassword(email);
        } catch (Exception e) {
            // Log the error internally, but don't tell the user why it failed
            log.error("Forgot password failed for: {}", email, e);
        }
        // Always return this to keep the user guessing
        return ResponseEntity.ok("If an account is associated with this email and is a local account, a reset link has been sent.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok("Password has been successfully reset.");
    }

    @DeleteMapping("/delete-account")
    public ResponseEntity<String> deleteAccount() {
        String userId = getCurrentUserId();

        // Prevent Guest users from triggering account deletion
        if ("GUEST".equals(userId)) {
            return ResponseEntity.status(401).body("Authentication required.");
        }

        authService.deleteUserAccount(userId);
        return ResponseEntity.ok("Account and all associated data deleted successfully.");
    }

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() &&
                !(auth instanceof AnonymousAuthenticationToken)) {
            // getName() returns the 24-char Hex ID from CustomUserDetailsService
            return auth.getName();
        }
        return "GUEST";
    }
}