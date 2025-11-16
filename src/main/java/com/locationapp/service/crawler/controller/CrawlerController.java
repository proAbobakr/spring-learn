package com.locationapp.service.crawler.controller;

import com.locationapp.service.crawler.config.CrawlerConfig;
import com.locationapp.service.crawler.service.DocumentationCrawlerService;
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
 * REST controller for Android documentation crawler
 */
@Slf4j
@RestController
@RequestMapping("/api/crawler")
@RequiredArgsConstructor
@Tag(name = "Android Documentation Crawler", description = "Crawl and process Android developer documentation")
public class CrawlerController {

    private final DocumentationCrawlerService crawlerService;
    private final CrawlerConfig config;

    private CompletableFuture<DocumentationCrawlerService.CrawlResult> currentCrawl;

    @PostMapping("/start")
    @Operation(summary = "Start documentation crawl",
            description = "Begins crawling Android documentation and generating outputs")
    public ResponseEntity<Map<String, Object>> startCrawl() {
        if (currentCrawl != null && !currentCrawl.isDone()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "A crawl is already in progress"));
        }

        log.info("Starting new crawl via REST API");

        // Run crawl asynchronously
        currentCrawl = CompletableFuture.supplyAsync(() -> {
            try {
                return crawlerService.executeCrawl();
            } catch (Exception e) {
                log.error("Error during async crawl", e);
                return DocumentationCrawlerService.CrawlResult.failed(e.getMessage());
            }
        });

        Map<String, Object> response = new HashMap<>();
        response.put("status", "started");
        response.put("message", "Crawl has been started. Use /api/crawler/status to check progress.");
        response.put("config", Map.of(
                "baseUrl", config.getBaseUrl(),
                "outputDirectory", config.getOutputDirectory(),
                "maxDepth", config.getMaxDepth()
        ));

        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    @Operation(summary = "Check crawl status",
            description = "Returns the current status of the crawl operation")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> response = new HashMap<>();

        if (currentCrawl == null) {
            response.put("status", "not_started");
            response.put("message", "No crawl has been initiated yet");
        } else if (currentCrawl.isDone()) {
            try {
                DocumentationCrawlerService.CrawlResult result = currentCrawl.get();
                response.put("status", result.isSuccess() ? "completed" : "failed");
                response.put("message", result.getMessage());

                if (result.isSuccess()) {
                    response.put("stats", Map.of(
                            "totalElements", result.getStats().getTotalElements(),
                            "urlsProcessed", result.getStats().getUrlsProcessed(),
                            "urlsFailed", result.getStats().getUrlsFailed(),
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
            description = "Returns the current crawler configuration")
    public ResponseEntity<CrawlerConfig> getConfig() {
        return ResponseEntity.ok(config);
    }

    @PutMapping("/config")
    @Operation(summary = "Update crawler configuration",
            description = "Updates crawler settings (requires restart for some changes)")
    public ResponseEntity<Map<String, String>> updateConfig(@RequestBody Map<String, Object> updates) {
        // Apply configuration updates
        if (updates.containsKey("maxDepth")) {
            config.setMaxDepth((Integer) updates.get("maxDepth"));
        }
        if (updates.containsKey("requestDelayMs")) {
            config.setRequestDelayMs((Integer) updates.get("requestDelayMs"));
        }
        if (updates.containsKey("outputDirectory")) {
            config.setOutputDirectory((String) updates.get("outputDirectory"));
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
