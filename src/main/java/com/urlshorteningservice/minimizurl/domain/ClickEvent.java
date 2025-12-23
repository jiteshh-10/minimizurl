package com.urlshorteningservice.minimizurl.domain;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "click_events") // Explicitly naming the collection
public class ClickEvent {

    @Id
    private String id;

    private Long urlId;
    private String referer;
    private String userAgent;

    @CreatedDate // Automatically set by Spring Data
    private Instant timestamp;

    // No-args constructor required by Spring Data/MongoDB
    public ClickEvent() {}

    public ClickEvent(Long urlId, String referer, String userAgent) {
        this.urlId = urlId;
        this.referer = referer;
        this.userAgent = userAgent;
    }
}