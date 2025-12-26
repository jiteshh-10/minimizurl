package com.urlshorteningservice.minimizurl.service;

import com.urlshorteningservice.minimizurl.config.JwtUtils;
import com.urlshorteningservice.minimizurl.domain.User;
import com.urlshorteningservice.minimizurl.dto.RegisterRequest;
import com.urlshorteningservice.minimizurl.exception.DuplicateUsernameException;
import com.urlshorteningservice.minimizurl.exception.InvalidLoginException;
import com.urlshorteningservice.minimizurl.exception.InvalidTokenException;
import com.urlshorteningservice.minimizurl.exception.UserNotFoundException;
import com.urlshorteningservice.minimizurl.repository.ClickEventRepository;
import com.urlshorteningservice.minimizurl.repository.UrlMappingRepository;
import com.urlshorteningservice.minimizurl.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final EmailService emailService;
    private final UrlMappingRepository urlMappingRepository;
    private final ClickEventRepository clickEventRepository;

    public User register(RegisterRequest request) {
        String hashedPw = passwordEncoder.encode(request.getPassword());

        // Use the @Builder from your User class to include the email
        User user = User.builder()
                .username(request.getUsername())
                .password(hashedPw)
                .email(request.getEmail())
                .provider("LOCAL")
                .roles(Set.of("ROLE_USER"))
                .build();

        return userRepository.save(user);
    }

    public String login(String username, String password) {
        // 3. Find user or throw error
        User user = userRepository.findByUsername(username)
                .orElseThrow(InvalidLoginException::new);
        // 4. Verify password
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new InvalidLoginException();
        }
        return jwtUtils.generateToken(user.getId());
    }

    public void processForgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + email));

        // Industry Practice: Only LOCAL users have passwords to reset
        if ("GOOGLE".equals(user.getProvider())) {
            throw new IllegalStateException("Accounts linked with Google must manage passwords through Google settings.");
        }

        // Generate and save token
        String token = UUID.randomUUID().toString();
        user.setPasswordResetToken(token);
        user.setPasswordResetTokenExpiry(Instant.now().plus(15, ChronoUnit.MINUTES));
        userRepository.save(user);

        // Send the actual email
        emailService.sendPasswordResetEmail(user.getEmail(), token);
    }

    public void resetPassword(String token, String newPassword) {
        // 1. Find user by token
        User user = userRepository.findByPasswordResetToken(token)
                .orElseThrow(() -> new InvalidTokenException("The reset link is invalid."));

        // 2. Security Check: Expiration
        if (user.getPasswordResetTokenExpiry().isBefore(Instant.now())) {
            throw new InvalidTokenException("The reset link has expired (15-minute limit).");
        }

        // 3. Security Check: Provider Validation
        if ("GOOGLE".equals(user.getProvider())) {
            throw new IllegalStateException("OAuth2 users must reset passwords via Google.");
        }

        // 4. Persistence
        user.setPassword(passwordEncoder.encode(newPassword));

        // 5. Token Invalidation (Crucial: Prevents Replay Attacks)
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);

        userRepository.save(user);
    }

    public void deleteUserAccount(String userId) {
        // 1. Verify existence
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException("User not found: " + userId);
        }

        // 2. Cascade Delete: Analytics
        clickEventRepository.deleteByOwnerId(userId);

        // 3. Cascade Delete: URL Mappings
        urlMappingRepository.deleteByUserId(userId);

        // 4. Delete the User record
        userRepository.deleteById(userId);
    }
}