package com.urlshorteningservice.minimizurl.repository;

import com.urlshorteningservice.minimizurl.domain.ClickEvent;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ClickEventRepository extends MongoRepository<ClickEvent, String> {
    void deleteByOwnerId(String userId); // Deletes all analytics events for those links
}
