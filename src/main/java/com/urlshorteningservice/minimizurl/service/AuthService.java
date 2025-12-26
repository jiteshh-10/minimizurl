package com.urlshorteningservice.minimizurl.service;

import com.urlshorteningservice.minimizurl.config.JwtUtils;
import com.urlshorteningservice.minimizurl.domain.User;
import com.urlshorteningservice.minimizurl.exception.DuplicateUsernameException;
import com.urlshorteningservice.minimizurl.exception.InvalidLoginException;
import com.urlshorteningservice.minimizurl.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    public User register(String username, String password) {
        // 1. Check if the username is already taken
        if (userRepository.findByUsername(username).isPresent()) {
            throw new DuplicateUsernameException(username);
        }

        // 2. Hash and save
        String hashedPw = passwordEncoder.encode(password);
        User user = new User(username, hashedPw, Set.of("ROLE_USER"));
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
}