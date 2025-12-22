package com.urlshorteningservice.minimizurl.repository;

import com.urlshorteningservice.minimizurl.domain.DatabaseSequence;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DatabaseSequenceRepository extends MongoRepository<DatabaseSequence, String> {
}
