package com.urlshorteningservice.minimizurl.domain;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "database_sequences")
@Data
public class DatabaseSequence {
    @Id
    private String url_sequence;
    private long seq;
}
