package com.locationapp.service.crawler.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Statistics about crawled documentation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentationStats {

    @Builder.Default
    private Map<AndroidCategory, Integer> categoryCounts = new HashMap<>();

    @Builder.Default
    private Map<AndroidDocType, Integer> typeCounts = new HashMap<>();

    private int totalElements;
    private int totalPackages;
    private int totalClasses;
    private int totalInterfaces;
    private int totalMethods;
    private int totalFields;

    private long crawlDurationMs;
    private int urlsProcessed;
    private int urlsFailed;

    public void incrementCategory(AndroidCategory category) {
        categoryCounts.merge(category, 1, Integer::sum);
    }

    public void incrementType(AndroidDocType type) {
        typeCounts.merge(type, 1, Integer::sum);
    }
}
