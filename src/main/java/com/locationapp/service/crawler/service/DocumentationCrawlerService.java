package com.locationapp.service.crawler.service;

import com.locationapp.service.crawler.config.CrawlerConfig;
import com.locationapp.service.crawler.export.LLMExporter;
import com.locationapp.service.crawler.export.MarkdownExporter;
import com.locationapp.service.crawler.model.AndroidCategory;
import com.locationapp.service.crawler.model.AndroidDocElement;
import com.locationapp.service.crawler.model.DocumentationStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Main orchestrator service for Android documentation crawling and processing
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentationCrawlerService {

    private final CrawlerConfig config;
    private final AndroidDocCrawler crawler;
    private final ContentGenerator contentGenerator;
    private final MarkdownExporter markdownExporter;
    private final LLMExporter llmExporter;

    /**
     * Execute the complete documentation crawling and processing pipeline
     */
    public CrawlResult executeCrawl() {
        log.info("=== Starting Android Documentation Crawl ===");
        log.info("Base URL: {}", config.getBaseUrl());
        log.info("Output Directory: {}", config.getOutputDirectory());

        long startTime = System.currentTimeMillis();

        try {
            // Step 1: Crawl documentation
            log.info("Step 1/4: Crawling Android documentation...");
            Map<AndroidCategory, List<AndroidDocElement>> categorizedDocs = crawler.crawl();

            if (categorizedDocs.isEmpty()) {
                log.warn("No documentation was crawled. Please check configuration and network connectivity.");
                return CrawlResult.failed("No documentation was crawled");
            }

            log.info("Crawled {} categories with {} total elements",
                    categorizedDocs.size(),
                    categorizedDocs.values().stream().mapToInt(List::size).sum());

            // Step 2: Enhance with generated content
            log.info("Step 2/4: Generating explanations and examples...");
            enhanceDocumentation(categorizedDocs);

            // Step 3: Export to markdown with diagrams
            if (config.isGenerateMarkdown()) {
                log.info("Step 3/4: Exporting to Markdown format...");
                markdownExporter.export(categorizedDocs, crawler.getStats());
            }

            // Step 4: Export to LLM-friendly format
            if (config.isGenerateLlmFormat()) {
                log.info("Step 4/4: Exporting to LLM-friendly format...");
                llmExporter.export(categorizedDocs);
            }

            // Create summary report
            createSummaryReport(categorizedDocs, crawler.getStats());

            long durationMs = System.currentTimeMillis() - startTime;
            log.info("=== Crawl Completed Successfully in {} seconds ===",
                    durationMs / 1000.0);

            return CrawlResult.success(categorizedDocs, crawler.getStats(), durationMs);

        } catch (Exception e) {
            log.error("Error during crawl execution", e);
            return CrawlResult.failed("Error: " + e.getMessage());
        }
    }

    /**
     * Enhance all documentation with generated content
     */
    private void enhanceDocumentation(Map<AndroidCategory, List<AndroidDocElement>> categorizedDocs) {
        int totalElements = categorizedDocs.values().stream().mapToInt(List::size).sum();
        int processed = 0;

        for (Map.Entry<AndroidCategory, List<AndroidDocElement>> entry : categorizedDocs.entrySet()) {
            for (AndroidDocElement element : entry.getValue()) {
                if (config.isGenerateExamples() || config.isGenerateExplanations()) {
                    contentGenerator.enhanceElement(element);
                }

                processed++;
                if (processed % 10 == 0) {
                    log.debug("Enhanced {}/{} elements", processed, totalElements);
                }
            }
        }

        log.info("Enhanced {} elements with generated content", totalElements);
    }

    /**
     * Create a summary report of the crawl
     */
    private void createSummaryReport(Map<AndroidCategory, List<AndroidDocElement>> docs,
                                      DocumentationStats stats) {
        try {
            File outputDir = new File(config.getOutputDirectory());
            FileUtils.forceMkdir(outputDir);

            StringBuilder report = new StringBuilder();

            report.append("# Android Documentation Crawl Summary\n\n");
            report.append(String.format("**Generated:** %s\n\n", java.time.LocalDateTime.now()));

            report.append("## Statistics\n\n");
            report.append(String.format("- **Total Elements:** %d\n", stats.getTotalElements()));
            report.append(String.format("- **Categories:** %d\n", docs.size()));
            report.append(String.format("- **URLs Processed:** %d\n", stats.getUrlsProcessed()));
            report.append(String.format("- **URLs Failed:** %d\n", stats.getUrlsFailed()));
            report.append(String.format("- **Processing Time:** %.2f seconds\n\n",
                    stats.getCrawlDurationMs() / 1000.0));

            report.append("## Category Breakdown\n\n");
            report.append("| Category | Count |\n");
            report.append("|----------|-------|\n");

            docs.entrySet().stream()
                    .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
                    .forEach(entry -> {
                        report.append(String.format("| %s | %d |\n",
                                entry.getKey().getDisplayName(),
                                entry.getValue().size()));
                    });

            report.append("\n## Output Locations\n\n");
            report.append(String.format("- **Markdown Documentation:** `%s/markdown/`\n",
                    config.getOutputDirectory()));
            report.append(String.format("- **LLM Format:** `%s/llm-format/`\n",
                    config.getOutputDirectory()));
            report.append(String.format("- **JSONL for RAG:** `%s/llm-format/android-docs.jsonl`\n",
                    config.getOutputDirectory()));
            report.append(String.format("- **Embeddings Format:** `%s/llm-format/embeddings-ready.jsonl`\n\n",
                    config.getOutputDirectory()));

            report.append("## Next Steps\n\n");
            report.append("1. Review the generated documentation in the markdown directory\n");
            report.append("2. Use the JSONL files to feed your local LLM\n");
            report.append("3. Import embeddings-ready.jsonl into your vector database\n");
            report.append("4. Check individual category files in llm-format/by-category/\n");

            File summaryFile = new File(outputDir, "CRAWL_SUMMARY.md");
            FileUtils.writeStringToFile(summaryFile, report.toString(), StandardCharsets.UTF_8);

            log.info("Summary report created: {}", summaryFile.getAbsolutePath());

        } catch (IOException e) {
            log.error("Error creating summary report", e);
        }
    }

    /**
     * Result of a crawl operation
     */
    @lombok.Data
    @lombok.Builder
    public static class CrawlResult {
        private boolean success;
        private String message;
        private Map<AndroidCategory, List<AndroidDocElement>> documentation;
        private DocumentationStats stats;
        private long durationMs;

        public static CrawlResult success(Map<AndroidCategory, List<AndroidDocElement>> docs,
                                           DocumentationStats stats, long durationMs) {
            return CrawlResult.builder()
                    .success(true)
                    .message("Crawl completed successfully")
                    .documentation(docs)
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
