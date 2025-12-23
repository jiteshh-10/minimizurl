package com.urlshorteningservice.minimizurl.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "url_mappings")
@Data
@NoArgsConstructor // Required by MongoDB to create objects
public class UrlMapping {

    @Id
    private long id;

    private String originalUrl;

    private long clicks;

    @Indexed(expireAfter = "0s") // TTL index to auto-delete expired documents
    private Instant expirationDate;

    private Instant createdDate;

    @Indexed(unique = true, sparse = true)
    private String customCode;

    // Custom Constructor for our Service
    public UrlMapping(long id, String originalUrl, Instant expirationDate) {
        this.id = id;
        this.originalUrl = originalUrl;
        this.expirationDate = expirationDate;
        this.clicks = 0;
        this.createdDate = Instant.now();
    }
}