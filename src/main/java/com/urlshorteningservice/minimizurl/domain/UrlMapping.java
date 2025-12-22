package com.urlshorteningservice.minimizurl.domain;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "url_mappings")
@Data
public class UrlMapping {
    @Id
    private long id;
    private String originalUrl;
    private long clicks;
}
