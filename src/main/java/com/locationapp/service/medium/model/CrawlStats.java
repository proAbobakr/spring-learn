package com.locationapp.service.medium.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Statistics for Medium crawl operation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrawlStats {

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private long durationMs;

    private int totalArticles;
    private int articlesAfterFilter;
    private int articlesFailed;

    @Builder.Default
    private Map<ArticleCategory, Integer> categoryCounts = new HashMap<>();

    private LocalDateTime oldestArticle;
    private LocalDateTime newestArticle;

    private int totalClaps;
    private int totalResponses;
    private int averageReadingTime;

    public void incrementCategory(ArticleCategory category) {
        categoryCounts.merge(category, 1, Integer::sum);
    }
}
