package com.urlshorteningservice.minimizurl.controller;

import com.urlshorteningservice.minimizurl.domain.UrlMapping;
import com.urlshorteningservice.minimizurl.dto.updateUrlRequest;
import com.urlshorteningservice.minimizurl.repository.UserRepository;
import com.urlshorteningservice.minimizurl.service.AnalyticsService;
import com.urlshorteningservice.minimizurl.service.UrlService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/mini")
@RequiredArgsConstructor
public class UrlController {

    private final UrlService urlService;
    private final AnalyticsService analyticsService;
    private final UserRepository userRepository;

    @PostMapping("/shorten")
    public ResponseEntity<String> shortenUrl(
            @RequestParam String url,
            @RequestParam(required = false) String customCode) {

        String result;
        if (customCode != null && !customCode.trim().isEmpty()) {
            result = urlService.shortenUrl(url, customCode);
        } else {
            result = urlService.shortenUrl(url);
        }

        // Note: We'll eventually move 'result == null' into an exception in UrlService too
        if (result == null) {
            return ResponseEntity.status(409).body("Error: Custom code is already in use.");
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{shortCode}")
    public void redirectUrl(
            @PathVariable String shortCode,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            @RequestHeader(value = "Referer", required = false) String referer,
            HttpServletResponse response) throws IOException {

        // Service throws LinkNotFoundException if mapping is missing
        String visitorId = getCurrentUserId();
        String originalUrl = urlService.getOriginalUrl(shortCode, referer, userAgent, visitorId);

        if (!originalUrl.startsWith("http")) {
            originalUrl = "https://" + originalUrl;
        }
        response.sendRedirect(originalUrl);
    }

    @GetMapping("/stats/{shortCode}")
    public ResponseEntity<UrlMapping> getStats(@PathVariable String shortCode) {
        // Service should throw LinkNotFoundException if not found
        UrlMapping mapping = urlService.getStats(shortCode);
        return ResponseEntity.ok(mapping);
    }

    @DeleteMapping("/{shortCode}")
    public ResponseEntity<Void> deleteUrl(@PathVariable String shortCode) {
        // Service handles finding and ownership check
        urlService.deleteById(shortCode, getCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{shortCode}")
    public ResponseEntity<UrlMapping> updateUrl(
            @PathVariable String shortCode,
            @RequestBody updateUrlRequest request) {

        // Service handles finding, ownership, and update logic
        UrlMapping updatedMapping = urlService.updateUrl(shortCode, request.getNewUrl(), getCurrentUserId());
        return ResponseEntity.ok(updatedMapping);
    }

    @GetMapping("/{shortCode}/fstats")
    public ResponseEntity<Map<String, Object>> getFullAnalytics(@PathVariable String shortCode) {
        // 1. We move the lookup and security logic to a new method in UrlService
        // Let's call it 'getMappingForUser'
        UrlMapping mapping = urlService.getMappingForUser(shortCode, getCurrentUserId());

        Long trueId = mapping.getId();

        // 2. The rest of the logic remains clean and focused on data gathering
        Map<String, Object> response = new HashMap<>();
        response.put("summary", analyticsService.getClickStats(trueId));
        response.put("topReferrers", analyticsService.getTopReferrers(trueId));
        response.put("deviceBreakdown", analyticsService.getDeviceStats(trueId));
        response.put("dailyTrend", analyticsService.getDailyClickTrend(trueId));

        return ResponseEntity.ok(response);
    }

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            // Since CustomUserDetailsService now sets the ID as the 'username' in the Principal
            return auth.getName();
        }
        return "GUEST";
    }
}
