package com.urlshorteningservice.minimizurl.controller;

import com.urlshorteningservice.minimizurl.domain.UrlMapping;
import com.urlshorteningservice.minimizurl.dto.updateUrlRequest;
import com.urlshorteningservice.minimizurl.service.AnalyticsService;
import com.urlshorteningservice.minimizurl.service.ShorteningService;
import com.urlshorteningservice.minimizurl.service.UrlService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;

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
    public void redirectUrl(
            @PathVariable String shortCode,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            @RequestHeader(value = "Referer", required = false) String referer,
            HttpServletResponse response) throws IOException {

        // 1. Call the updated service with metadata
        String originalUrl = urlService.getOriginalUrl(shortCode, referer, userAgent);

        if (originalUrl != null) {
            // 2. Your existing logic to ensure valid protocol
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

    @RestController
    @RequestMapping("/api/analytics")
    @RequiredArgsConstructor
    public class AnalyticsController {

        private final AnalyticsService analyticsService;
        private final ShorteningService shorteningService; // To decode shortCode to ID

        @GetMapping("/{shortCode}/stats")
        public ResponseEntity<Map<String, Object>> getFullAnalytics(@PathVariable String shortCode) {
            // 1. Get the actual mapping from the DB to find the TRUE ID
            UrlMapping mapping = urlService.findMappingByShortCode(shortCode);

            if (mapping == null) return ResponseEntity.notFound().build();

            Long trueId = mapping.getId();

            // 2. Query stats using the verified ID
            Map<String, Object> response = new HashMap<>();
            response.put("summary", analyticsService.getClickStats(trueId));
            response.put("topReferrers", analyticsService.getTopReferrers(trueId));
            response.put("deviceBreakdown", analyticsService.getDeviceStats(trueId));
            response.put("dailyTrend", analyticsService.getDailyClickTrend(trueId));

            return ResponseEntity.ok(response);
        }
    }

}
