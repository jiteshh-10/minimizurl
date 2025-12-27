package com.urlshorteningservice.minimizurl.repository;

import com.urlshorteningservice.minimizurl.domain.UrlMapping;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UrlMappingRepository extends MongoRepository<UrlMapping, Long> {
    boolean existsByCustomCode(String customCode);
    void deleteByUserId(String userId); // Deletes all links owned by the user
    long countByUserId(String userId);
}
