package com.urlshorteningservice.minimizurl.controller;

import com.urlshorteningservice.minimizurl.domain.UrlMapping;
import com.urlshorteningservice.minimizurl.service.UrlService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/mini") // No trailing slash here
@RequiredArgsConstructor
public class UrlController {

    private final UrlService urlService;

    @PostMapping("/shorten") // Added leading slash for consistency
    public String shortenUrl(@RequestBody String originalUrl) { // Added @RequestBody
        return urlService.shortenUrl(originalUrl);
    }

    @GetMapping("/{shortCode}")
    public void redirectUrl(@PathVariable String shortCode, HttpServletResponse response) throws IOException {
        String originalUrl = urlService.getOriginalUrl(shortCode);

        if (originalUrl != null) {
            // Ensure the URL starts with http/https for the redirect to work externally
            if (!originalUrl.startsWith("http")) {
                originalUrl = "https://" + originalUrl;
            }
            response.sendRedirect(originalUrl);
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @GetMapping("/stats/{shortCode}")
    public ResponseEntity<UrlMapping> getStats(@PathVariable String shortCode) {
        UrlMapping mapping = urlService.getStats(shortCode);

        if (mapping != null) {
            // Return 200 OK with the object in the body
            return ResponseEntity.ok(mapping);
        } else {
            // Return 404 Not Found with an empty body
            return ResponseEntity.notFound().build();
        }
    }
}
