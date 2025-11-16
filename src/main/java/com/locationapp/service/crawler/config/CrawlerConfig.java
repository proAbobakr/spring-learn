package com.locationapp.service.crawler.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for Android documentation crawler
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "android.crawler")
public class CrawlerConfig {

    // Base URLs
    private String baseUrl = "https://developer.android.com/reference";
    private String kotlinUrl = "https://developer.android.com/reference/kotlin";

    // Crawler settings
    private int maxDepth = 3;
    private int maxPagesPerCategory = 100;
    private int requestDelayMs = 1000; // Polite crawling
    private int maxRetries = 3;
    private int timeoutSeconds = 30;
    private boolean useSelenium = false; // Set to true if dynamic content is needed

    // User agent
    private String userAgent = "Mozilla/5.0 (Educational Android Documentation Crawler)";

    // Output settings
    private String outputDirectory = "./android-docs-output";
    private boolean generateMarkdown = true;
    private boolean generateJson = true;
    private boolean generateLlmFormat = true;
    private boolean generateDiagrams = true;

    // Packages to crawl (empty = all)
    private List<String> targetPackages = new ArrayList<>();

    // Categories to focus on (empty = all)
    private List<String> targetCategories = new ArrayList<>();

    // Content generation
    private boolean generateExamples = true;
    private boolean generateExplanations = true;
    private boolean generateUseCases = true;
}
