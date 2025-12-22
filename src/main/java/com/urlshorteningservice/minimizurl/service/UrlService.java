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

    public String shortenUrl(String originalUrl) {
        long id = sequenceGeneratorService.generateSequence("url_sequence");
        String shortCode = shorteningService.encode(id);

        // Calculate expiration: 30 days from now
        Instant expiry = Instant.now().plus(DEFAULT_EXPIRY_DAYS, ChronoUnit.DAYS);

        UrlMapping mapping = new UrlMapping(id, originalUrl, expiry);
        urlMappingRepository.save(mapping);

        return shortCode;
    }

    public String getOriginalUrl(String shortCode) {
        long id = shorteningService.decode(shortCode);

        // Locate the document by ID
        Query query = new Query(Criteria.where("_id").is(id));

        // Sliding Expiration: Increment clicks AND reset the 30-day timer
        Instant newExpiry = Instant.now().plus(DEFAULT_EXPIRY_DAYS, ChronoUnit.DAYS);
        Update update = new Update()
                .inc("clicks", 1)
                .set("expirationDate", newExpiry);

        UrlMapping mapping = mongoTemplate.findAndModify(
                query,
                update,
                UrlMapping.class
        );

        return (mapping != null) ? mapping.getOriginalUrl() : null;
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