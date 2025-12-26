package com.urlshorteningservice.minimizurl.service;

import com.urlshorteningservice.minimizurl.domain.ClickEvent;
import com.urlshorteningservice.minimizurl.domain.UrlMapping;
import com.urlshorteningservice.minimizurl.domain.User;
import com.urlshorteningservice.minimizurl.exception.LinkNotFoundException;
import com.urlshorteningservice.minimizurl.exception.UnauthorizedAccessException;
import com.urlshorteningservice.minimizurl.repository.ClickEventRepository;
import com.urlshorteningservice.minimizurl.repository.UrlMappingRepository;
import com.urlshorteningservice.minimizurl.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final ClickEventRepository clickEventRepository;
    private final UserRepository userRepository;

    // Default expiration: 30 days
    private static final long DEFAULT_EXPIRY_DAYS = 30;

    // Method A: Standard Auto-Generated Shortening (Updated)
    public String shortenUrl(String originalUrl) {
        // 1. Identity Check
        String userId = getCurrentUserId();

        // 2. Logic as before
        long id = sequenceGeneratorService.generateSequence("url_sequence");
        String shortCode = shorteningService.encode(id);

        UrlMapping mapping = new UrlMapping(id, originalUrl, calculateExpiry());
        mapping.setUserId(userId); // Persist the identity
        urlMappingRepository.save(mapping);

        return shortCode;
    }

    // Method B: Custom Shortening (Updated)
    public String shortenUrl(String originalUrl, String customCode) {
        if (urlMappingRepository.existsByCustomCode(customCode)) {
            return null;
        }

        // 1. Identity Check
        String userId = getCurrentUserId();

        long id = sequenceGeneratorService.generateSequence("url_sequence");

        UrlMapping mapping = new UrlMapping(id, originalUrl, calculateExpiry());
        mapping.setCustomCode(customCode);
        mapping.setUserId(userId); // Persist the identity 🔗
        urlMappingRepository.save(mapping);

        return customCode;
    }

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            // Since CustomUserDetailsService now sets the ID as the 'username' in the Principal
            return auth.getName();
        }
        return "GUEST";
    }

    public String getOriginalUrl(String shortCode, String referer, String userAgent, String visitorId) {
        UrlMapping mapping = findMappingByShortCode(shortCode);

        // Use custom exception instead of returning null
        if (mapping == null) {
            throw new LinkNotFoundException(shortCode);
        }

        // Atomically update click count and expiry
        Query query = new Query(Criteria.where("_id").is(mapping.getId()));
        Update update = new Update()
                .inc("clicks", 1)
                .set("expirationDate", calculateExpiry());

        mongoTemplate.updateFirst(query, update, UrlMapping.class);

        recordClick(mapping.getId(), mapping.getUserId(), visitorId, referer, userAgent);

        return mapping.getOriginalUrl();
    }

    private Instant calculateExpiry() {
        return Instant.now().plus(DEFAULT_EXPIRY_DAYS, ChronoUnit.DAYS);
    }

    public UrlMapping getStats(String shortCode) {
        UrlMapping mapping = findMappingByShortCode(shortCode);
        if (mapping == null) {
            throw new LinkNotFoundException(shortCode);
        }
        return mapping;
    }

    // Method A: Secure Deletion
    public void deleteById(String shortCode, String userId) {
        // Validation and ownership are now handled by our helper
        UrlMapping mapping = getMappingForUser(shortCode, userId);
        urlMappingRepository.deleteById(mapping.getId());
    }

    // Method B: Secure Update
    public UrlMapping updateUrl(String shortCode, String newUrl, String userId) {
        // Reuse helper for validation
        UrlMapping mapping = getMappingForUser(shortCode, userId);

        mapping.setOriginalUrl(newUrl);
        mapping.setExpirationDate(calculateExpiry());
        return urlMappingRepository.save(mapping);
    }

    @Async
    public void recordClick(Long urlId, String ownerId, String visitorId, String referer, String userAgent) {
        ClickEvent event = new ClickEvent(urlId, ownerId, visitorId, referer, userAgent);
        clickEventRepository.save(event);
    }

    public UrlMapping findMappingByShortCode(String shortCode) {
        long id = -1;
        try {
            id = shorteningService.decode(shortCode);
        } catch (Exception e) {
            // Likely a custom code
        }

        Query query = new Query(new Criteria().orOperator(
                Criteria.where("_id").is(id),
                Criteria.where("customCode").is(shortCode)
        ));

        return mongoTemplate.findOne(query, UrlMapping.class);
    }

    public UrlMapping getMappingForUser(String shortCode, String userId) {
        UrlMapping mapping = findMappingByShortCode(shortCode);
        if (mapping == null) {
            throw new LinkNotFoundException(shortCode);
        }
        if ("GUEST".equals(userId) || !mapping.getUserId().equals(userId)) {
            throw new UnauthorizedAccessException();
        }
        return mapping;
    }
}