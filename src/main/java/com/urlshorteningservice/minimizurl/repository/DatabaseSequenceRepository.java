package com.urlshorteningservice.minimizurl.repository;

import com.urlshorteningservice.minimizurl.domain.DatabaseSequence;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DatabaseSequenceRepository extends MongoRepository<DatabaseSequence, String> {
}
