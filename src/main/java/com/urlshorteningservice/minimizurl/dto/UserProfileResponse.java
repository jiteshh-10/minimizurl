package com.urlshorteningservice.minimizurl.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserProfileResponse {
    private String username;
    private String email;
    private String fullName;
    private String provider;
    private long totalLinksCreated;
}
