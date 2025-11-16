package com.locationapp.service.medium.model;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * Configuration for Medium ProAndroidDev crawler
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "medium.crawler")
public class MediumCrawlerConfig {

    // Publication details
    private String publicationId = "c72404660798";
    private String publicationName = "ProAndroidDev";
    private String publicationUrl = "https://proandroiddev.com";

    // Crawler settings
    private int maxArticles = 100;
    private int requestDelayMs = 2000; // Polite crawling
    private int maxRetries = 3;
    private int timeoutSeconds = 30;
    private String userAgent = "Mozilla/5.0 (Educational Medium Crawler)";

    // Date filtering
    private String filterAfterDate = "2024-08-01T00:00:00"; // August 2024
    private boolean onlyRecentArticles = true;

    // Content extraction
    private boolean extractFullText = true;
    private boolean extractImages = true;
    private boolean extractCodeSnippets = true;

    // Summarization
    private boolean generateSummaries = true;
    private boolean generateTldr = true;
    private boolean generateKeyTakeaways = true;
    private int summaryMaxLength = 500;

    // Output settings
    private String outputDirectory = "./medium-articles-output";
    private boolean generateMarkdown = true;
    private boolean generateJson = true;
    private boolean generateLlmFormat = true;

    /**
     * Get filter date as LocalDateTime
     */
    public LocalDateTime getFilterDate() {
        return LocalDateTime.parse(filterAfterDate);
    }
}
