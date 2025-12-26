package com.urlshorteningservice.minimizurl.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public void sendPasswordResetEmail(String toEmail, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Password Reset Request - MinimizUrl");

        String resetLink = frontendUrl + "/reset-password?token=" + token;

        message.setText("Hello,\n\n" +
                "You requested a password reset for your MinimizUrl account.\n" +
                "Click the link below to set a new password. This link will expire in 15 minutes:\n\n" +
                resetLink + "\n\n" +
                "If you did not request this, please ignore this email.");

        mailSender.send(message);
    }
}
