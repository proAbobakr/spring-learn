package com.locationapp.service.medium.service;

import com.locationapp.service.medium.export.MediumMarkdownExporter;
import com.locationapp.service.medium.export.MediumLLMExporter;
import com.locationapp.service.medium.model.CrawlStats;
import com.locationapp.service.medium.model.MediumArticle;
import com.locationapp.service.medium.model.MediumCrawlerConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Main orchestrator for Medium article crawling and processing
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MediumCrawlerService {

    private final MediumCrawlerConfig config;
    private final MediumCrawler crawler;
    private final ArticleSummarizer summarizer;
    private final MediumMarkdownExporter markdownExporter;
    private final MediumLLMExporter llmExporter;

    /**
     * Execute complete crawl and export pipeline
     */
    public CrawlResult executeCrawl() {
        log.info("=== Starting ProAndroidDev Medium Crawl ===");
        log.info("Publication: {}", config.getPublicationName());
        log.info("Filter date: {}", config.getFilterAfterDate());

        long startTime = System.currentTimeMillis();
        LocalDateTime startDateTime = LocalDateTime.now();

        try {
            // Step 1: Crawl articles
            log.info("Step 1/3: Crawling articles...");
            List<MediumArticle> articles = crawler.crawl();

            if (articles.isEmpty()) {
                log.warn("No articles were crawled");
                return CrawlResult.failed("No articles found");
            }

            log.info("Crawled {} articles", articles.size());

            // Step 2: Enhance with summaries
            log.info("Step 2/3: Generating summaries and analysis...");
            enhanceArticles(articles);

            // Step 3: Export
            if (config.isGenerateMarkdown()) {
                log.info("Step 3a/3: Exporting to Markdown...");
                markdownExporter.export(articles);
            }

            if (config.isGenerateLlmFormat()) {
                log.info("Step 3b/3: Exporting to LLM format...");
                llmExporter.export(articles);
            }

            // Generate statistics
            CrawlStats stats = generateStats(articles, startDateTime);

            long durationMs = System.currentTimeMillis() - startTime;
            log.info("=== Crawl Completed Successfully in {} seconds ===",
                    durationMs / 1000.0);

            return CrawlResult.success(articles, stats, durationMs);

        } catch (Exception e) {
            log.error("Error during crawl execution", e);
            return CrawlResult.failed("Error: " + e.getMessage());
        }
    }

    private void enhanceArticles(List<MediumArticle> articles) {
        int processed = 0;
        for (MediumArticle article : articles) {
            summarizer.enhanceArticle(article);
            processed++;
            if (processed % 10 == 0) {
                log.debug("Enhanced {}/{} articles", processed, articles.size());
            }
        }
        log.info("Enhanced {} articles with summaries", articles.size());
    }

    private CrawlStats generateStats(List<MediumArticle> articles, LocalDateTime startTime) {
        CrawlStats.CrawlStatsBuilder stats = CrawlStats.builder();

        stats.startTime(startTime);
        stats.endTime(LocalDateTime.now());
        stats.totalArticles(articles.size());
        stats.articlesAfterFilter(articles.size());

        // Calculate date range
        articles.stream()
                .filter(a -> a.getPublishedDate() != null)
                .min(Comparator.comparing(MediumArticle::getPublishedDate))
                .ifPresent(a -> stats.oldestArticle(a.getPublishedDate()));

        articles.stream()
                .filter(a -> a.getPublishedDate() != null)
                .max(Comparator.comparing(MediumArticle::getPublishedDate))
                .ifPresent(a -> stats.newestArticle(a.getPublishedDate()));

        // Count by category
        articles.forEach(article -> {
            if (article.getCategory() != null) {
                stats.build().incrementCategory(article.getCategory());
            }
        });

        // Calculate totals
        int totalClaps = articles.stream().mapToInt(MediumArticle::getClaps).sum();
        int totalResponses = articles.stream().mapToInt(MediumArticle::getResponses).sum();
        int avgReadingTime = articles.isEmpty() ? 0 :
                articles.stream().mapToInt(MediumArticle::getReadingTimeMinutes).sum() / articles.size();

        stats.totalClaps(totalClaps);
        stats.totalResponses(totalResponses);
        stats.averageReadingTime(avgReadingTime);

        return stats.build();
    }

    /**
     * Result of crawl operation
     */
    @lombok.Data
    @lombok.Builder
    public static class CrawlResult {
        private boolean success;
        private String message;
        private List<MediumArticle> articles;
        private CrawlStats stats;
        private long durationMs;

        public static CrawlResult success(List<MediumArticle> articles,
                                           CrawlStats stats, long durationMs) {
            return CrawlResult.builder()
                    .success(true)
                    .message("Crawl completed successfully")
                    .articles(articles)
                    .stats(stats)
                    .durationMs(durationMs)
                    .build();
        }

        public static CrawlResult failed(String message) {
            return CrawlResult.builder()
                    .success(false)
                    .message(message)
                    .build();
        }
    }
}
