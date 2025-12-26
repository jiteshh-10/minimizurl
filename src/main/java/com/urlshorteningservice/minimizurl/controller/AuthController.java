package com.urlshorteningservice.minimizurl.controller;

import com.urlshorteningservice.minimizurl.dto.RegisterRequest;
import com.urlshorteningservice.minimizurl.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest request) {
        // We removed the try-catch. If registration fails (e.g., user exists),
        // the Service will throw an exception caught by GlobalExceptionHandler.
        authService.register(request.getUsername(), request.getPassword());
        return ResponseEntity.ok("User registered successfully!");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody RegisterRequest request) {
        // No more manual 401 response here.
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
}