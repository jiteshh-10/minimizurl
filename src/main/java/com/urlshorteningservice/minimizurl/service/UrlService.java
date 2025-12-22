package com.urlshorteningservice.minimizurl.service;

import com.urlshorteningservice.minimizurl.domain.UrlMapping;
import com.urlshorteningservice.minimizurl.repository.UrlMappingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UrlService {

    private final UrlMappingRepository urlMappingRepository;
    private final ShorteningService shorteningService;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final MongoTemplate mongoTemplate;

    public String shortenUrl(String originalUrl) {
        // Step 1: Get the next unique ID from the sequence generator
        long id = sequenceGeneratorService.generateSequence("url_sequence");

        // Step 2: Encode that ID into a short Base62 string
        String shortCode = shorteningService.encode(id);

        // Step 3: Create the mapping and save it to MongoDB
        UrlMapping mapping = new UrlMapping();
        mapping.setId(id);
        mapping.setOriginalUrl(originalUrl);
        urlMappingRepository.save(mapping);

        // Step 4: Return the short code to the user
        return shortCode;

    }

    public String getOriginalUrl(String shortCode) {
        // Step 1: Decode the short code back to the unique ID
        long id = shorteningService.decode(shortCode);

        Query query = new Query(Criteria.where("_id").is(id));
        Update update = new Update().inc("clicks", 1);
        // Step 2: // FindAndModify handles the increment and retrieval in one step
        UrlMapping mapping = mongoTemplate.findAndModify(
                query,
                update,
                UrlMapping.class
        );

        // Step 3: Return the original URL or null if not found
        return (mapping != null) ? mapping.getOriginalUrl() : null;
    }


}
