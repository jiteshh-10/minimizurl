package com.urlshorteningservice.minimizurl.controller;

import com.urlshorteningservice.minimizurl.domain.UrlMapping;
import com.urlshorteningservice.minimizurl.dto.updateUrlRequest;
import com.urlshorteningservice.minimizurl.service.UrlService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/mini") // No trailing slash here
@RequiredArgsConstructor
public class UrlController {

    private final UrlService urlService;

    @PostMapping("/shorten")
    public ResponseEntity<String> shortenUrl(
            @RequestParam String url,
            @RequestParam(required = false) String customCode) {

        String result;

        // 1. Check if a custom code was provided
        if (customCode != null && !customCode.trim().isEmpty()) {
            result = urlService.shortenUrl(url, customCode);
        } else {
            result = urlService.shortenUrl(url);
        }

        // 2. Handle the "Already Taken" case
        if (result == null) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Error: Custom code '" + customCode + "' is already in use.");
        }

        return ResponseEntity.ok(result);
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

    @DeleteMapping("/{shortCode}")
    public ResponseEntity<Void> deleteUrl(@PathVariable String shortCode) {
        urlService.deleteById(shortCode); // The action
        return ResponseEntity.noContent().build(); // The response
    }

    @PutMapping("/{shortCode}")
    public ResponseEntity<UrlMapping> updateUrl(
            @PathVariable String shortCode,
            @RequestBody updateUrlRequest request
    ) {
        // We get the URL out of the DTO
        String newUrl = request.getNewUrl();

        // Now we call the service
        UrlMapping updatedMapping = urlService.updateUrl(shortCode, newUrl);

        // How should we handle the response if updatedMapping is null?
        if (updatedMapping != null) {
            return ResponseEntity.ok(updatedMapping);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

}
