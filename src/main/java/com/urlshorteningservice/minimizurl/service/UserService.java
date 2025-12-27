package com.urlshorteningservice.minimizurl.service;

import com.urlshorteningservice.minimizurl.domain.User;
import com.urlshorteningservice.minimizurl.dto.UserProfileResponse;
import com.urlshorteningservice.minimizurl.exception.UserNotFoundException;
import com.urlshorteningservice.minimizurl.repository.UrlMappingRepository;
import com.urlshorteningservice.minimizurl.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UrlMappingRepository urlMappingRepository;

    public UserProfileResponse getUserProfile(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        long linkCount = urlMappingRepository.countByUserId(userId);

        return UserProfileResponse.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .provider(user.getProvider())
                .totalLinksCreated(linkCount)
                .build();
    }
}
