package com.urlshorteningservice.minimizurl.repository;

import com.urlshorteningservice.minimizurl.domain.UrlMapping;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UrlMappingRepository extends MongoRepository<UrlMapping, Long> {
}
