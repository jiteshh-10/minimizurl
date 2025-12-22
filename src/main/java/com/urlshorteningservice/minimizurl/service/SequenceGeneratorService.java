package com.urlshorteningservice.minimizurl.service;

import com.urlshorteningservice.minimizurl.domain.DatabaseSequence;
import com.urlshorteningservice.minimizurl.repository.DatabaseSequenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.data.mongodb.core.MongoOperations;
import static org.springframework.data.mongodb.core.FindAndModifyOptions.options;

@Service
@RequiredArgsConstructor
public class SequenceGeneratorService {

    private final DatabaseSequenceRepository databaseSequenceRepository;

    private final MongoOperations mongoOperations;

    public long generateSequence(String seqName){
        // 1. Create a query to find the specific sequence by ID
        Query query = new Query(Criteria.where("_id").is(seqName));

        // 2. Define the update: increment the 'seq' field by 1
        Update update = new Update().inc("seq", 1);

        // 3. Execute findAndModify
        DatabaseSequence counter = mongoOperations.findAndModify(
                query,
                update,
                options().returnNew(true).upsert(true),
                DatabaseSequence.class
        );

        return (counter != null) ? counter.getSeq() : 1;
    }
}
