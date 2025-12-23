package com.urlshorteningservice.minimizurl.service;

import com.urlshorteningservice.minimizurl.domain.ClickEvent;
import com.urlshorteningservice.minimizurl.repository.ClickEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final MongoTemplate mongoTemplate;
    private ClickEventRepository clickEventRepository;

    public Map<String, Object> getClickStats(Long urlId) {
        Query query = new Query(Criteria.where("urlId").is(urlId));

        long totalClicks = mongoTemplate.count(query, ClickEvent.class);

        // Find the most recent click
        query.with(Sort.by(Sort.Direction.DESC, "timestamp")).limit(1);
        ClickEvent lastClick = mongoTemplate.findOne(query, ClickEvent.class);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalClicks", totalClicks);
        stats.put("lastClick", lastClick != null ? lastClick.getTimestamp() : null);

        return stats;
    }

    public List<Map> getTopReferrers(Long urlId) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("urlId").is(urlId)),
                // Stage: If referer is null, replace with "Direct"
                Aggregation.project()
                        .and(ConditionalOperators.ifNull("referer").then("Direct")).as("referer"),
                Aggregation.group("referer").count().as("count"),
                Aggregation.sort(Sort.Direction.DESC, "count"),
                Aggregation.limit(5)
        );

        return mongoTemplate.aggregate(aggregation, "click_events", Map.class)
                .getMappedResults();
    }

    public List<Map> getDeviceStats(Long urlId) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("urlId").is(urlId)),
                Aggregation.project()
                        .andExpression("cond(indexOfCP(userAgent, 'Mobi') >= 0, 'Mobile', 'Desktop')")
                        .as("deviceType"),
                Aggregation.group("deviceType").count().as("count")
        );

        return mongoTemplate.aggregate(aggregation, "click_events", Map.class)
                .getMappedResults();
    }

    public List<Map> getDailyClickTrend(Long urlId) {
        // 1. Calculate the date 7 days ago
        Instant sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS);

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("urlId").is(urlId)
                        .and("timestamp").gte(sevenDaysAgo)),
                // Extracting just the date (YYYY-MM-DD) from the timestamp
                Aggregation.project()
                        .and("timestamp").dateAsFormattedString("%Y-%m-%d").as("date"),
                Aggregation.group("date").count().as("count"),
                Aggregation.sort(Sort.Direction.ASC, "_id") // Sort by date
        );

        return mongoTemplate.aggregate(aggregation, "click_events", Map.class)
                .getMappedResults();
    }
}
