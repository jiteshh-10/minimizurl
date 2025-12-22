package com.urlshorteningservice.minimizurl.repository;

import com.urlshorteningservice.minimizurl.domain.UrlMapping;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UrlMappingRepository extends MongoRepository<UrlMapping, Long> {
    // New method to check if a custom code already exists
    boolean existsByCustomCode(String customCode);

    // Optional: useful if we need to find by custom code without the OR query
    Optional<UrlMapping> findByCustomCode(String customCode);
}
