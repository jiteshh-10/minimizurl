package com.urlshorteningservice.minimizurl.service;

import com.urlshorteningservice.minimizurl.domain.UrlMapping;
import com.urlshorteningservice.minimizurl.repository.UrlMappingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class UrlService {

    private final UrlMappingRepository urlMappingRepository;
    private final ShorteningService shorteningService;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final MongoTemplate mongoTemplate;

    // Default expiration: 30 days
    private static final long DEFAULT_EXPIRY_DAYS = 30;

    // Method A: Standard Auto-Generated Shortening
    public String shortenUrl(String originalUrl) {
        long id = sequenceGeneratorService.generateSequence("url_sequence");
        String shortCode = shorteningService.encode(id);

        UrlMapping mapping = new UrlMapping(id, originalUrl, calculateExpiry());
        urlMappingRepository.save(mapping);

        return shortCode;
    }

    // Method B: Custom Shortening (Overloaded)
    public String shortenUrl(String originalUrl, String customCode) {
        // 1. 🛡Check if customCode is already taken
        if (urlMappingRepository.existsByCustomCode(customCode)) {
            return null; // Controller can handle this as a 409 Conflict
        }

        // 2. Generate numeric ID for consistency
        long id = sequenceGeneratorService.generateSequence("url_sequence");

        // 3. Save with customCode
        UrlMapping mapping = new UrlMapping(id, originalUrl, calculateExpiry());
        mapping.setId(id);
        mapping.setOriginalUrl(originalUrl);
        mapping.setCustomCode(customCode);

        urlMappingRepository.save(mapping);
        return customCode;
    }

    public String getOriginalUrl(String shortCode) {
        // Dual-Field Lookup Strategy

        // Step 1: Try to decode as a numeric ID (Standard links)
        long id = -1;
        try {
            id = shorteningService.decode(shortCode);
        } catch (Exception e) {
            // Not a valid Base62 string? Likely a custom code.
        }

        // Step 2: Query MongoDB for either the ID OR the customCode
        Query query = new Query(new Criteria().orOperator(
                Criteria.where("_id").is(id),
                Criteria.where("customCode").is(shortCode)
        ));

        Instant newExpiry = calculateExpiry();
        Update update = new Update()
                .inc("clicks", 1)
                .set("expirationDate", newExpiry);

        UrlMapping mapping = mongoTemplate.findAndModify(query, update, UrlMapping.class);

        return (mapping != null) ? mapping.getOriginalUrl() : null;
    }

    private Instant calculateExpiry() {
        return Instant.now().plus(DEFAULT_EXPIRY_DAYS, ChronoUnit.DAYS);
    }

    public UrlMapping getStats(String shortCode) {
        long id = shorteningService.decode(shortCode);
        return urlMappingRepository.findById(id).orElse(null);
    }

    public void deleteById(String shortCode) {
        long id = shorteningService.decode(shortCode);
        urlMappingRepository.deleteById(id);
    }

    public UrlMapping updateUrl(String shortCode, String newUrl) {
        long id = shorteningService.decode(shortCode);
        UrlMapping mapping = urlMappingRepository.findById(id).orElse(null);
        if (mapping != null) {
            mapping.setOriginalUrl(newUrl);

            // Optional: Reset expiration when the URL is manually updated?
            mapping.setExpirationDate(Instant.now().plus(DEFAULT_EXPIRY_DAYS, ChronoUnit.DAYS));

            urlMappingRepository.save(mapping);
        }
        return mapping;
    }
}