package com.urlshorteningservice.minimizurl.repository;

import com.urlshorteningservice.minimizurl.domain.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email); // For Google OAuth2 📧
    Optional<User> findByUsernameOrEmail(String username, String email);
}