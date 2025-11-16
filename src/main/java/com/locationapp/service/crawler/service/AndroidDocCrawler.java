package com.locationapp.service.crawler.service;

import com.locationapp.service.crawler.config.CrawlerConfig;
import com.locationapp.service.crawler.model.AndroidCategory;
import com.locationapp.service.crawler.model.AndroidDocElement;
import com.locationapp.service.crawler.model.AndroidDocType;
import com.locationapp.service.crawler.model.DocumentationStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Main crawler service for Android documentation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AndroidDocCrawler {

    private final CrawlerConfig config;
    private final Set<String> visitedUrls = ConcurrentHashMap.newKeySet();
    private final Map<AndroidCategory, List<AndroidDocElement>> categorizedDocs = new ConcurrentHashMap<>();
    private final DocumentationStats stats = DocumentationStats.builder().build();

    /**
     * Start crawling Android documentation
     */
    public Map<AndroidCategory, List<AndroidDocElement>> crawl() {
        log.info("Starting Android documentation crawl from: {}", config.getBaseUrl());
        long startTime = System.currentTimeMillis();

        try {
            // Start with main reference page
            crawlMainReferencePage();

            // Crawl specific packages if configured
            if (!config.getTargetPackages().isEmpty()) {
                for (String packageName : config.getTargetPackages()) {
                    crawlPackage(packageName);
                }
            }

        } catch (Exception e) {
            log.error("Error during crawl", e);
        }

        stats.setTotalElements(categorizedDocs.values().stream()
                .mapToInt(List::size).sum());
        stats.setCrawlDurationMs(System.currentTimeMillis() - startTime);
        stats.setUrlsProcessed(visitedUrls.size());

        log.info("Crawl completed. Processed {} URLs in {} ms",
                visitedUrls.size(), stats.getCrawlDurationMs());

        return categorizedDocs;
    }

    /**
     * Crawl the main Android reference page to get package list
     */
    private void crawlMainReferencePage() {
        try {
            Document doc = fetchDocument(config.getBaseUrl());
            if (doc == null) return;

            // Find all package links
            Elements packageLinks = doc.select("a[href*='/reference/']");

            log.info("Found {} potential package links", packageLinks.size());

            int count = 0;
            for (Element link : packageLinks) {
                if (count >= config.getMaxPagesPerCategory()) break;

                String href = link.attr("abs:href");
                if (href.contains("/packages") || href.contains("/kotlin/packages")) {
                    crawlPackageListPage(href);
                    count++;
                }

                sleep(config.getRequestDelayMs());
            }

        } catch (Exception e) {
            log.error("Error crawling main reference page", e);
        }
    }

    /**
     * Crawl a package list page
     */
    private void crawlPackageListPage(String url) {
        try {
            Document doc = fetchDocument(url);
            if (doc == null) return;

            // Find class/interface links
            Elements classLinks = doc.select("a[href*='/reference/']");

            for (Element link : classLinks) {
                String href = link.attr("abs:href");
                String text = link.text();

                // Check if it's a class page
                if (isClassPage(href) && !text.isEmpty()) {
                    crawlClassPage(href);
                    sleep(config.getRequestDelayMs());
                }
            }

        } catch (Exception e) {
            log.error("Error crawling package list: {}", url, e);
        }
    }

    /**
     * Crawl a specific package
     */
    private void crawlPackage(String packageName) {
        String url = config.getBaseUrl() + "/" + packageName.replace(".", "/");
        log.info("Crawling package: {}", packageName);
        crawlPackageListPage(url);
    }

    /**
     * Crawl a class/interface documentation page
     */
    private void crawlClassPage(String url) {
        try {
            Document doc = fetchDocument(url);
            if (doc == null) return;

            AndroidDocElement element = parseClassPage(doc, url);
            if (element != null) {
                AndroidCategory category = element.getCategory();
                categorizedDocs.computeIfAbsent(category, k -> new ArrayList<>())
                        .add(element);

                stats.incrementCategory(category);
                stats.incrementType(element.getType());

                log.debug("Parsed: {} ({})", element.getName(), category.getDisplayName());
            }

        } catch (Exception e) {
            log.error("Error crawling class page: {}", url, e);
            stats.setUrlsFailed(stats.getUrlsFailed() + 1);
        }
    }

    /**
     * Parse a class documentation page
     */
    private AndroidDocElement parseClassPage(Document doc, String url) {
        try {
            // Extract basic information
            String title = doc.select("h1, .api-title").first() != null ?
                    doc.select("h1, .api-title").first().text() : "";

            if (title.isEmpty()) {
                return null;
            }

            // Determine package name from URL or breadcrumbs
            String packageName = extractPackageName(doc, url);
            AndroidCategory category = AndroidCategory.categorizeByPackage(packageName);

            // Determine type (class, interface, etc.)
            AndroidDocType type = determineDocType(doc, title);

            // Build the element
            AndroidDocElement.AndroidDocElementBuilder builder = AndroidDocElement.builder()
                    .name(title)
                    .fullName(packageName + "." + title)
                    .packageName(packageName)
                    .type(type)
                    .category(category)
                    .url(url)
                    .summary(extractSummary(doc))
                    .description(extractDescription(doc))
                    .syntax(extractSyntax(doc))
                    .examples(extractExamples(doc))
                    .methods(new ArrayList<>())
                    .fields(new ArrayList<>());

            // Extract inheritance info
            extractInheritance(doc, builder);

            // Extract methods
            extractMethods(doc, builder, packageName);

            // Extract fields
            extractFields(doc, builder, packageName);

            return builder.build();

        } catch (Exception e) {
            log.error("Error parsing class page: {}", url, e);
            return null;
        }
    }

    private String extractPackageName(Document doc, String url) {
        // Try breadcrumbs first
        Element breadcrumb = doc.selectFirst(".devsite-breadcrumb-item");
        if (breadcrumb != null) {
            String text = breadcrumb.text();
            if (text.contains(".")) {
                return text;
            }
        }

        // Extract from URL
        String path = url.replace(config.getBaseUrl(), "");
        String[] parts = path.split("/");
        List<String> packageParts = new ArrayList<>();

        for (String part : parts) {
            if (!part.isEmpty() && !part.equals("reference") &&
                    !part.equals("kotlin") && !part.contains(".html")) {
                if (Character.isLowerCase(part.charAt(0))) {
                    packageParts.add(part);
                } else {
                    break;
                }
            }
        }

        return String.join(".", packageParts);
    }

    private AndroidDocType determineDocType(Document doc, String title) {
        String pageContent = doc.text().toLowerCase();

        if (pageContent.contains("interface " + title.toLowerCase()) ||
                title.toLowerCase().startsWith("interface")) {
            return AndroidDocType.INTERFACE;
        }
        if (pageContent.contains("enum " + title.toLowerCase()) ||
                title.toLowerCase().endsWith("enum")) {
            return AndroidDocType.ENUM;
        }
        if (pageContent.contains("@interface") || title.startsWith("@")) {
            return AndroidDocType.ANNOTATION;
        }

        return AndroidDocType.CLASS;
    }

    private String extractSummary(Document doc) {
        Element summary = doc.selectFirst(".api-summary, .short-description, p");
        return summary != null ? summary.text() : "";
    }

    private String extractDescription(Document doc) {
        Element description = doc.selectFirst(".api-description, .long-description");
        return description != null ? description.text() : extractSummary(doc);
    }

    private String extractSyntax(Document doc) {
        Element syntax = doc.selectFirst("pre, code, .api-signature, .syntax");
        return syntax != null ? syntax.text() : "";
    }

    private List<AndroidDocElement.CodeExample> extractExamples(Document doc) {
        List<AndroidDocElement.CodeExample> examples = new ArrayList<>();

        Elements codeBlocks = doc.select("pre code, .code-sample");
        for (Element code : codeBlocks) {
            String language = code.hasClass("kotlin") || code.text().contains("fun ") ?
                    "kotlin" : "java";

            examples.add(AndroidDocElement.CodeExample.builder()
                    .language(language)
                    .code(code.text())
                    .build());

            if (examples.size() >= 3) break; // Limit examples
        }

        return examples;
    }

    private void extractInheritance(Document doc, AndroidDocElement.AndroidDocElementBuilder builder) {
        Element inheritance = doc.selectFirst(".inheritance, .extends");
        if (inheritance != null) {
            String text = inheritance.text();
            if (text.contains("extends")) {
                String superClass = text.substring(text.indexOf("extends") + 7).trim();
                builder.superClass(superClass.split("\\s+")[0]);
            }
        }
    }

    private void extractMethods(Document doc, AndroidDocElement.AndroidDocElementBuilder builder,
                                 String packageName) {
        Elements methodSections = doc.select(".api-item, .method, tr[id*='method']");
        List<AndroidDocElement> methods = new ArrayList<>();

        for (Element method : methodSections) {
            String methodName = method.select(".api-item-title, .method-name, code").text();
            if (methodName.isEmpty()) continue;

            methods.add(AndroidDocElement.builder()
                    .name(methodName)
                    .type(AndroidDocType.METHOD)
                    .packageName(packageName)
                    .summary(method.select("p, .description").text())
                    .build());

            if (methods.size() >= 50) break; // Limit methods per class
        }

        builder.methods(methods);
    }

    private void extractFields(Document doc, AndroidDocElement.AndroidDocElementBuilder builder,
                                String packageName) {
        Elements fieldSections = doc.select(".field, tr[id*='field'], tr[id*='constant']");
        List<AndroidDocElement> fields = new ArrayList<>();

        for (Element field : fieldSections) {
            String fieldName = field.select("code, .field-name").text();
            if (fieldName.isEmpty()) continue;

            fields.add(AndroidDocElement.builder()
                    .name(fieldName)
                    .type(AndroidDocType.FIELD)
                    .packageName(packageName)
                    .summary(field.select("p, .description").text())
                    .build());

            if (fields.size() >= 30) break; // Limit fields
        }

        builder.fields(fields);
    }

    private Document fetchDocument(String url) {
        if (visitedUrls.contains(url)) {
            return null;
        }

        for (int retry = 0; retry < config.getMaxRetries(); retry++) {
            try {
                log.debug("Fetching: {} (attempt {})", url, retry + 1);

                Document doc = Jsoup.connect(url)
                        .userAgent(config.getUserAgent())
                        .timeout(config.getTimeoutSeconds() * 1000)
                        .followRedirects(true)
                        .get();

                visitedUrls.add(url);
                return doc;

            } catch (IOException e) {
                log.warn("Failed to fetch {} (attempt {}): {}",
                        url, retry + 1, e.getMessage());

                if (retry < config.getMaxRetries() - 1) {
                    sleep(config.getRequestDelayMs() * (retry + 1));
                }
            }
        }

        stats.setUrlsFailed(stats.getUrlsFailed() + 1);
        return null;
    }

    private boolean isClassPage(String href) {
        return href.contains("/reference/") &&
                !href.contains("/packages") &&
                !href.endsWith("/") &&
                (href.contains(".html") || href.matches(".*[A-Z][a-zA-Z0-9]*$"));
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public DocumentationStats getStats() {
        return stats;
    }

    public Map<AndroidCategory, List<AndroidDocElement>> getCategorizedDocs() {
        return categorizedDocs;
    }
}
