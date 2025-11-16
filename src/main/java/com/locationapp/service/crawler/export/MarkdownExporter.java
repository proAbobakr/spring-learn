package com.locationapp.service.crawler.export;

import com.locationapp.service.crawler.config.CrawlerConfig;
import com.locationapp.service.crawler.model.AndroidCategory;
import com.locationapp.service.crawler.model.AndroidDocElement;
import com.locationapp.service.crawler.model.AndroidDocType;
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
import java.util.stream.Collectors;

/**
 * Exports documentation to human-readable Markdown format with diagrams
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarkdownExporter {

    private final CrawlerConfig config;

    /**
     * Export all documentation to Markdown files
     */
    public void export(Map<AndroidCategory, List<AndroidDocElement>> categorizedDocs,
                       DocumentationStats stats) {
        try {
            File outputDir = new File(config.getOutputDirectory(), "markdown");
            FileUtils.forceMkdir(outputDir);

            log.info("Exporting to Markdown: {}", outputDir.getAbsolutePath());

            // Create index page
            createIndexPage(outputDir, categorizedDocs, stats);

            // Create category pages
            for (Map.Entry<AndroidCategory, List<AndroidDocElement>> entry : categorizedDocs.entrySet()) {
                createCategoryPage(outputDir, entry.getKey(), entry.getValue());
            }

            // Create individual API pages
            for (Map.Entry<AndroidCategory, List<AndroidDocElement>> entry : categorizedDocs.entrySet()) {
                createIndividualPages(outputDir, entry.getKey(), entry.getValue());
            }

            log.info("Markdown export completed successfully");

        } catch (IOException e) {
            log.error("Error exporting to Markdown", e);
        }
    }

    /**
     * Create index page with overview and statistics
     */
    private void createIndexPage(File outputDir, Map<AndroidCategory, List<AndroidDocElement>> docs,
                                   DocumentationStats stats) throws IOException {
        StringBuilder md = new StringBuilder();

        md.append("# Android API Documentation\n\n");
        md.append("Comprehensive Android development reference for Kotlin/Java developers.\n\n");

        // Statistics
        md.append("## 📊 Statistics\n\n");
        md.append("| Metric | Value |\n");
        md.append("|--------|-------|\n");
        md.append(String.format("| Total APIs | %d |\n", stats.getTotalElements()));
        md.append(String.format("| Categories | %d |\n", docs.size()));
        md.append(String.format("| URLs Processed | %d |\n", stats.getUrlsProcessed()));
        md.append(String.format("| Processing Time | %.2f seconds |\n",
                stats.getCrawlDurationMs() / 1000.0));
        md.append("\n");

        // Category breakdown chart (Mermaid)
        md.append("## 📈 Category Distribution\n\n");
        md.append("```mermaid\n");
        md.append("pie title API Distribution by Category\n");

        docs.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
                .limit(10)
                .forEach(entry -> {
                    String categoryName = entry.getKey().getDisplayName()
                            .replace("\"", "")
                            .replace("&", "and");
                    md.append(String.format("    \"%s\" : %d\n",
                            categoryName, entry.getValue().size()));
                });

        md.append("```\n\n");

        // Type distribution
        md.append("## 🔤 Type Distribution\n\n");
        md.append("```mermaid\n");
        md.append("pie title API Distribution by Type\n");

        stats.getTypeCounts().entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .forEach(entry -> {
                    md.append(String.format("    \"%s\" : %d\n",
                            entry.getKey().getDisplayName(), entry.getValue()));
                });

        md.append("```\n\n");

        // Categories list
        md.append("## 📚 Categories\n\n");

        docs.entrySet().stream()
                .sorted((a, b) -> a.getKey().getDisplayName()
                        .compareTo(b.getKey().getDisplayName()))
                .forEach(entry -> {
                    AndroidCategory category = entry.getKey();
                    int count = entry.getValue().size();
                    String filename = sanitizeFilename(category.name()) + ".md";

                    md.append(String.format("- [%s](./%s) - %d APIs\n",
                            category.getDisplayName(), filename, count));
                });

        md.append("\n");

        // Quick links
        md.append("## 🔗 Quick Links\n\n");
        md.append("- [All Classes](./all-classes.md)\n");
        md.append("- [All Interfaces](./all-interfaces.md)\n");
        md.append("- [Package Index](./package-index.md)\n");
        md.append("\n");

        FileUtils.writeStringToFile(new File(outputDir, "README.md"),
                md.toString(), StandardCharsets.UTF_8);
    }

    /**
     * Create a page for each category
     */
    private void createCategoryPage(File outputDir, AndroidCategory category,
                                      List<AndroidDocElement> elements) throws IOException {
        StringBuilder md = new StringBuilder();

        md.append("# ").append(category.getDisplayName()).append("\n\n");
        md.append(String.format("Total APIs: %d\n\n", elements.size()));

        // Category description
        md.append("## Overview\n\n");
        md.append(getCategoryDescription(category)).append("\n\n");

        // Relationship diagram
        if (config.isGenerateDiagrams()) {
            md.append("## 🔀 API Relationships\n\n");
            md.append(generateCategoryDiagram(category, elements));
            md.append("\n");
        }

        // APIs by type
        Map<AndroidDocType, List<AndroidDocElement>> byType = elements.stream()
                .collect(Collectors.groupingBy(AndroidDocElement::getType));

        for (Map.Entry<AndroidDocType, List<AndroidDocElement>> entry : byType.entrySet()) {
            md.append("## ").append(entry.getKey().getDisplayName()).append("s\n\n");

            entry.getValue().stream()
                    .sorted((a, b) -> a.getName().compareTo(b.getName()))
                    .forEach(element -> {
                        String filename = sanitizeFilename(element.getFullName()) + ".md";
                        md.append(String.format("### [%s](./%s/%s)\n\n",
                                element.getName(),
                                sanitizeFilename(category.name()),
                                filename));

                        if (element.getSummary() != null && !element.getSummary().isEmpty()) {
                            md.append(element.getSummary()).append("\n\n");
                        }

                        md.append(String.format("**Package:** `%s`\n\n", element.getPackageName()));
                    });
        }

        String filename = sanitizeFilename(category.name()) + ".md";
        FileUtils.writeStringToFile(new File(outputDir, filename),
                md.toString(), StandardCharsets.UTF_8);
    }

    /**
     * Create individual API documentation pages
     */
    private void createIndividualPages(File outputDir, AndroidCategory category,
                                        List<AndroidDocElement> elements) throws IOException {
        File categoryDir = new File(outputDir, sanitizeFilename(category.name()));
        FileUtils.forceMkdir(categoryDir);

        for (AndroidDocElement element : elements) {
            StringBuilder md = new StringBuilder();

            // Header
            md.append("# ").append(element.getName()).append("\n\n");

            // Metadata
            md.append("| Property | Value |\n");
            md.append("|----------|-------|\n");
            md.append(String.format("| **Type** | %s |\n", element.getType().getDisplayName()));
            md.append(String.format("| **Category** | %s |\n", category.getDisplayName()));
            md.append(String.format("| **Package** | `%s` |\n", element.getPackageName()));
            md.append(String.format("| **Full Name** | `%s` |\n", element.getFullName()));

            if (element.getUrl() != null) {
                md.append(String.format("| **Official Docs** | [View](%s) |\n", element.getUrl()));
            }
            md.append("\n");

            // Summary
            if (element.getSummary() != null && !element.getSummary().isEmpty()) {
                md.append("## Summary\n\n");
                md.append(element.getSummary()).append("\n\n");
            }

            // Description
            if (element.getDescription() != null && !element.getDescription().isEmpty()) {
                md.append("## Description\n\n");
                md.append(element.getDescription()).append("\n\n");
            }

            // AI Explanation
            if (element.getAiExplanation() != null && !element.getAiExplanation().isEmpty()) {
                md.append("## Detailed Explanation\n\n");
                md.append(element.getAiExplanation()).append("\n\n");
            }

            // Syntax
            if (element.getSyntax() != null && !element.getSyntax().isEmpty()) {
                md.append("## Syntax\n\n");
                md.append("```kotlin\n");
                md.append(element.getSyntax()).append("\n");
                md.append("```\n\n");
            }

            // Code Examples
            if (element.getKotlinExample() != null || element.getJavaExample() != null) {
                md.append("## Examples\n\n");

                if (element.getKotlinExample() != null) {
                    md.append("### Kotlin\n\n");
                    md.append("```kotlin\n");
                    md.append(element.getKotlinExample());
                    md.append("```\n\n");
                }

                if (element.getJavaExample() != null) {
                    md.append("### Java\n\n");
                    md.append("```java\n");
                    md.append(element.getJavaExample());
                    md.append("```\n\n");
                }
            }

            // Use Cases
            if (element.getUseCases() != null && !element.getUseCases().isEmpty()) {
                md.append("## Use Cases\n\n");
                element.getUseCases().forEach(useCase ->
                        md.append("- ").append(useCase).append("\n"));
                md.append("\n");
            }

            // Best Practices
            if (element.getBestPractices() != null && !element.getBestPractices().isEmpty()) {
                md.append("## Best Practices\n\n");
                element.getBestPractices().forEach(practice ->
                        md.append("- ").append(practice).append("\n"));
                md.append("\n");
            }

            // Methods
            if (element.getMethods() != null && !element.getMethods().isEmpty()) {
                md.append("## Methods\n\n");
                element.getMethods().forEach(method -> {
                    md.append(String.format("### %s\n\n", method.getName()));
                    if (method.getSummary() != null) {
                        md.append(method.getSummary()).append("\n\n");
                    }
                });
            }

            // Related APIs
            if (element.getRelatedAPIs() != null && !element.getRelatedAPIs().isEmpty()) {
                md.append("## Related APIs\n\n");
                element.getRelatedAPIs().forEach(api ->
                        md.append("- `").append(api).append("`\n"));
                md.append("\n");
            }

            // Hierarchy diagram for classes
            if (element.getType() == AndroidDocType.CLASS && config.isGenerateDiagrams()) {
                md.append("## Class Hierarchy\n\n");
                md.append(generateHierarchyDiagram(element));
                md.append("\n");
            }

            String filename = sanitizeFilename(element.getFullName()) + ".md";
            FileUtils.writeStringToFile(new File(categoryDir, filename),
                    md.toString(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Generate Mermaid diagram for category relationships
     */
    private String generateCategoryDiagram(AndroidCategory category, List<AndroidDocElement> elements) {
        StringBuilder diagram = new StringBuilder();
        diagram.append("```mermaid\n");
        diagram.append("graph TD\n");

        // Show relationships between classes (simplified)
        elements.stream()
                .filter(e -> e.getType() == AndroidDocType.CLASS)
                .limit(20) // Limit to avoid overly complex diagrams
                .forEach(element -> {
                    String nodeName = sanitizeNodeName(element.getName());

                    if (element.getSuperClass() != null) {
                        String superName = sanitizeNodeName(element.getSuperClass());
                        diagram.append(String.format("    %s[%s] --> %s[%s]\n",
                                nodeName, element.getName(),
                                superName, element.getSuperClass()));
                    }
                });

        diagram.append("```\n");
        return diagram.toString();
    }

    /**
     * Generate class hierarchy diagram
     */
    private String generateHierarchyDiagram(AndroidDocElement element) {
        StringBuilder diagram = new StringBuilder();
        diagram.append("```mermaid\n");
        diagram.append("graph BT\n");

        String nodeName = sanitizeNodeName(element.getName());
        diagram.append(String.format("    %s[%s]\n", nodeName, element.getName()));

        if (element.getSuperClass() != null) {
            String superName = sanitizeNodeName(element.getSuperClass());
            diagram.append(String.format("    %s --> %s[%s]\n",
                    nodeName, superName, element.getSuperClass()));
        }

        if (element.getInterfaces() != null) {
            element.getInterfaces().forEach(iface -> {
                String ifaceName = sanitizeNodeName(iface);
                diagram.append(String.format("    %s -.-> %s[%s]\n",
                        nodeName, ifaceName, iface));
            });
        }

        diagram.append("```\n");
        return diagram.toString();
    }

    private String getCategoryDescription(AndroidCategory category) {
        return switch (category) {
            case UI_COMPONENTS -> "User interface components including views, widgets, and layouts for building Android UIs.";
            case ACTIVITIES_FRAGMENTS -> "Core components for managing app screens, lifecycle, and navigation.";
            case LIFECYCLE -> "Architecture components for managing UI-related data in a lifecycle-conscious way.";
            case NAVIGATION -> "Jetpack Navigation component for implementing navigation between destinations.";
            case DATA_STORAGE -> "APIs for storing and managing data including databases, preferences, and file storage.";
            case NETWORKING -> "Networking APIs for making HTTP requests and communicating with web services.";
            case MEDIA -> "APIs for working with media files, graphics, and multimedia content.";
            case SENSORS_LOCATION -> "APIs for accessing device sensors and location services.";
            case PERMISSIONS_SECURITY -> "APIs for handling permissions and implementing security features.";
            case BACKGROUND_TASKS -> "APIs for scheduling and executing background work.";
            case NOTIFICATIONS -> "APIs for creating and managing notifications.";
            case MATERIAL_DESIGN -> "Material Design components for building beautiful, consistent UIs.";
            case JETPACK_COMPOSE -> "Modern declarative UI toolkit for building native Android UIs.";
            case TESTING -> "Testing frameworks and utilities for Android apps.";
            case DEPENDENCY_INJECTION -> "Dependency injection frameworks for managing dependencies.";
            case COROUTINES -> "Kotlin coroutines for asynchronous programming.";
            case VIEWMODEL_LIVEDATA -> "Architecture components for managing UI state.";
            case RECYCLER_VIEW -> "Advanced list view component for efficient display of large datasets.";
            case INTENT_SERVICES -> "APIs for inter-component communication and background services.";
            case RESOURCES -> "APIs for accessing app resources like strings, drawables, and layouts.";
            case UTILITIES -> "General utility classes and helper methods.";
            case CORE -> "Core Android framework classes.";
            default -> "Android development APIs.";
        };
    }

    private String sanitizeFilename(String name) {
        return name.replaceAll("[^a-zA-Z0-9.-]", "_");
    }

    private String sanitizeNodeName(String name) {
        return name.replaceAll("[^a-zA-Z0-9]", "");
    }
}
