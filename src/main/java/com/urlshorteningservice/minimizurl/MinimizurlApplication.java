package com.urlshorteningservice.minimizurl;

import com.urlshorteningservice.minimizurl.service.UrlService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class MinimizurlApplication {
    public static void main(String[] args) {
        SpringApplication.run(MinimizurlApplication.class, args);
    }
}
