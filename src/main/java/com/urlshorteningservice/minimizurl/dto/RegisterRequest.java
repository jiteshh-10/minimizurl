package com.urlshorteningservice.minimizurl.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String password;
    @Email(message = "Please provide a valid email address")
    @NotBlank(message = "Email is required for account recovery")
    private String email;
}