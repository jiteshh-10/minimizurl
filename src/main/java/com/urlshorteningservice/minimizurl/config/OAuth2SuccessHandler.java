package com.urlshorteningservice.minimizurl.config;

import com.urlshorteningservice.minimizurl.domain.User;
import com.urlshorteningservice.minimizurl.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String googleId = oAuth2User.getAttribute("sub"); // Unique ID from Google

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = User.builder()
                    .username(email)
                    .email(email)
                    .fullName(name)
                    .googleId(googleId)
                    .provider("GOOGLE")
                    .roles(Collections.singleton("ROLE_USER"))
                    .build();
            return userRepository.save(newUser);
        });

        String token = jwtUtils.generateToken(user.getId());

        // REDIRECT TO YOUR ACTUAL AUTHCONTROLLER ENDPOINT
        // Redirect to frontend with token
        String frontendUrl = "http://localhost:3000/auth/callback?token=" + token;
        getRedirectStrategy().sendRedirect(request, response, frontendUrl);
    }

}