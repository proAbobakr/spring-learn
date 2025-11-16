package com.locationapp.service.medium.controller;

import com.locationapp.service.medium.model.MediumCrawlerConfig;
import com.locationapp.service.medium.service.MediumCrawlerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * REST controller for ProAndroidDev Medium crawler
 */
@Slf4j
@RestController
@RequestMapping("/api/medium-crawler")
@RequiredArgsConstructor
@Tag(name = "Medium Crawler", description = "Crawl and process ProAndroidDev Medium articles")
public class MediumCrawlerController {

    private final MediumCrawlerService crawlerService;
    private final MediumCrawlerConfig config;

    private CompletableFuture<MediumCrawlerService.CrawlResult> currentCrawl;

    @PostMapping("/start")
    @Operation(summary = "Start Medium article crawl",
            description = "Begins crawling ProAndroidDev articles with summarization")
    public ResponseEntity<Map<String, Object>> startCrawl() {
        if (currentCrawl != null && !currentCrawl.isDone()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "A crawl is already in progress"));
        }

        log.info("Starting ProAndroidDev crawl via REST API");

        currentCrawl = CompletableFuture.supplyAsync(() -> {
            try {
                return crawlerService.executeCrawl();
            } catch (Exception e) {
                log.error("Error during async crawl", e);
                return MediumCrawlerService.CrawlResult.failed(e.getMessage());
            }
        });

        Map<String, Object> response = new HashMap<>();
        response.put("status", "started");
        response.put("message", "Crawl has been started. Use /api/medium-crawler/status to check progress.");
        response.put("config", Map.of(
                "publication", config.getPublicationName(),
                "filterAfterDate", config.getFilterAfterDate(),
                "maxArticles", config.getMaxArticles(),
                "outputDirectory", config.getOutputDirectory()
        ));

        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    @Operation(summary = "Check crawl status",
            description = "Returns the current status of the Medium crawl operation")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> response = new HashMap<>();

        if (currentCrawl == null) {
            response.put("status", "not_started");
            response.put("message", "No crawl has been initiated yet");
        } else if (currentCrawl.isDone()) {
            try {
                MediumCrawlerService.CrawlResult result = currentCrawl.get();
                response.put("status", result.isSuccess() ? "completed" : "failed");
                response.put("message", result.getMessage());

                if (result.isSuccess()) {
                    response.put("stats", Map.of(
                            "totalArticles", result.getStats().getTotalArticles(),
                            "oldestArticle", result.getStats().getOldestArticle() != null ?
                                    result.getStats().getOldestArticle().toLocalDate() : "N/A",
                            "newestArticle", result.getStats().getNewestArticle() != null ?
                                    result.getStats().getNewestArticle().toLocalDate() : "N/A",
                            "totalClaps", result.getStats().getTotalClaps(),
                            "averageReadingTime", result.getStats().getAverageReadingTime(),
                            "durationMs", result.getDurationMs()
                    ));
                    response.put("outputDirectory", config.getOutputDirectory());
                }
            } catch (Exception e) {
                response.put("status", "error");
                response.put("message", e.getMessage());
            }
        } else {
            response.put("status", "in_progress");
            response.put("message", "Crawl is currently running");
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/config")
    @Operation(summary = "Get crawler configuration",
            description = "Returns the current Medium crawler configuration")
    public ResponseEntity<MediumCrawlerConfig> getConfig() {
        return ResponseEntity.ok(config);
    }

    @PutMapping("/config")
    @Operation(summary = "Update crawler configuration",
            description = "Updates crawler settings")
    public ResponseEntity<Map<String, String>> updateConfig(@RequestBody Map<String, Object> updates) {
        if (updates.containsKey("maxArticles")) {
            config.setMaxArticles((Integer) updates.get("maxArticles"));
        }
        if (updates.containsKey("requestDelayMs")) {
            config.setRequestDelayMs((Integer) updates.get("requestDelayMs"));
        }
        if (updates.containsKey("outputDirectory")) {
            config.setOutputDirectory((String) updates.get("outputDirectory"));
        }
        if (updates.containsKey("filterAfterDate")) {
            config.setFilterAfterDate((String) updates.get("filterAfterDate"));
        }

        return ResponseEntity.ok(Map.of(
                "status", "updated",
                "message", "Configuration has been updated"
        ));
    }

    @PostMapping("/stop")
    @Operation(summary = "Stop current crawl",
            description = "Attempts to stop the currently running crawl")
    public ResponseEntity<Map<String, String>> stopCrawl() {
        if (currentCrawl != null && !currentCrawl.isDone()) {
            currentCrawl.cancel(true);
            return ResponseEntity.ok(Map.of(
                    "status", "stopped",
                    "message", "Crawl has been stopped"
            ));
        }

        return ResponseEntity.badRequest()
                .body(Map.of("error", "No crawl is currently running"));
    }
}
