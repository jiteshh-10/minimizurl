package com.urlshorteningservice.minimizurl.service;

import com.urlshorteningservice.minimizurl.domain.User;
import com.urlshorteningservice.minimizurl.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;



@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        User user;
        // Check if it's a 24-char Hex ID (MongoDB ObjectId format)
        if (identifier.matches("^[0-9a-fA-F]{24}$")) {
            user = userRepository.findById(identifier)
                    .orElseThrow(() -> new UsernameNotFoundException("ID not found: " + identifier));
        } else {
            user = userRepository.findByUsername(identifier)
                    .orElseThrow(() -> new UsernameNotFoundException("Username not found: " + identifier));
        }

        String password = (user.getPassword() != null) ? user.getPassword() : "OAUTH2_PLACEHOLDER";
        String[] authorities = user.getRoles().stream()
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .toArray(String[]::new);

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getId()) // Crucial: Put the ID here!
                .password(password)
                .authorities(authorities)
                .build();
    }
}
