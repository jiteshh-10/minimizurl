package com.urlshorteningservice.minimizurl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableMongoAuditing
public class MinimizurlApplication {
    public static void main(String[] args) {
        SpringApplication.run(MinimizurlApplication.class, args);
    }
}
