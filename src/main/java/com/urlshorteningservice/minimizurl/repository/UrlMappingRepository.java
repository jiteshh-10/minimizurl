package com.urlshorteningservice.minimizurl.repository;

import com.urlshorteningservice.minimizurl.domain.UrlMapping;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UrlMappingRepository extends MongoRepository<UrlMapping, Long> {
    // Allows for more efficient ownership checks at the DB level
    Optional<UrlMapping> findByCustomCodeAndUserId(String customCode, String userId);
    boolean existsByCustomCode(String customCode);
    // Also add ID-based ownership lookup
    Optional<UrlMapping> findByIdAndUserId(Long id, String userId);
}
