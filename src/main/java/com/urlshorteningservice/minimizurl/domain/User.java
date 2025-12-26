package com.urlshorteningservice.minimizurl.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Set;

@Document(collection = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    private String id;

    @Indexed(unique = true)
    private String username;

    // Optional for Google users, required for Standard users
    private String password;

    @Indexed(unique = true)
    private String email; //  Essential for OAuth2 mapping

    private String fullName; // Capture user's name from Google profile

    private String provider; // "LOCAL" or "GOOGLE"

    private String googleId; // Unique ID provided by Google (sub claim)

    private Set<String> roles;

    private String passwordResetToken;
    private Instant passwordResetTokenExpiry;

    // Constructor for standard registration
    public User(String username, String password, Set<String> roles) {
        this.username = username;
        this.password = password;
        this.roles = roles;
        this.provider = "LOCAL";
    }
}