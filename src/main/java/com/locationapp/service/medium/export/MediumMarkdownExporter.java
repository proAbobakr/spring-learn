package com.locationapp.service.medium.export;

import com.locationapp.service.medium.model.ArticleCategory;
import com.locationapp.service.medium.model.MediumArticle;
import com.locationapp.service.medium.model.MediumCrawlerConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Exports Medium articles to Markdown format
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MediumMarkdownExporter {

    private final MediumCrawlerConfig config;

    public void export(List<MediumArticle> articles) {
        try {
            File outputDir = new File(config.getOutputDirectory(), "markdown");
            FileUtils.forceMkdir(outputDir);

            log.info("Exporting to Markdown: {}", outputDir.getAbsolutePath());

            // Create index
            createIndexPage(outputDir, articles);

            // Create category pages
            Map<ArticleCategory, List<MediumArticle>> byCategory = articles.stream()
                    .filter(a -> a.getCategory() != null)
                    .collect(Collectors.groupingBy(MediumArticle::getCategory));

            for (Map.Entry<ArticleCategory, List<MediumArticle>> entry : byCategory.entrySet()) {
                createCategoryPage(outputDir, entry.getKey(), entry.getValue());
            }

            // Create timeline page
            createTimelinePage(outputDir, articles);

            // Create individual article pages
            createIndividualPages(outputDir, articles);

            log.info("Markdown export completed");

        } catch (IOException e) {
            log.error("Error exporting to Markdown", e);
        }
    }

    private void createIndexPage(File outputDir, List<MediumArticle> articles) throws IOException {
        StringBuilder md = new StringBuilder();

        md.append("# ProAndroidDev Articles\n\n");
        md.append(String.format("**Total Articles:** %d  \n", articles.size()));
        md.append(String.format("**Date Range:** %s to %s  \n\n",
                articles.stream()
                        .filter(a -> a.getPublishedDate() != null)
                        .min(Comparator.comparing(MediumArticle::getPublishedDate))
                        .map(a -> a.getPublishedDate().toLocalDate().toString())
                        .orElse("Unknown"),
                articles.stream()
                        .filter(a -> a.getPublishedDate() != null)
                        .max(Comparator.comparing(MediumArticle::getPublishedDate))
                        .map(a -> a.getPublishedDate().toLocalDate().toString())
                        .orElse("Unknown")));

        // Category distribution
        Map<ArticleCategory, Long> categoryCounts = articles.stream()
                .filter(a -> a.getCategory() != null)
                .collect(Collectors.groupingBy(MediumArticle::getCategory, Collectors.counting()));

        md.append("## 📊 Articles by Category\n\n");
        md.append("| Category | Count |\n");
        md.append("|----------|-------|\n");

        categoryCounts.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .forEach(entry -> {
                    md.append(String.format("| [%s](./%s.md) | %d |\n",
                            entry.getKey().getDisplayName(),
                            sanitizeFilename(entry.getKey().name()),
                            entry.getValue()));
                });

        md.append("\n## 📅 Quick Links\n\n");
        md.append("- [All Articles by Date](./timeline.md)\n");

        categoryCounts.keySet().stream()
                .sorted(Comparator.comparing(ArticleCategory::getDisplayName))
                .forEach(category -> {
                    md.append(String.format("- [%s Articles](./%s.md)\n",
                            category.getDisplayName(),
                            sanitizeFilename(category.name())));
                });

        md.append("\n## ⭐ Recent Articles\n\n");

        articles.stream()
                .sorted(Comparator.comparing(MediumArticle::getPublishedDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(10)
                .forEach(article -> {
                    md.append(String.format("### [%s](./articles/%s.md)\n\n",
                            article.getTitle(),
                            sanitizeFilename(article.getId())));

                    if (article.getAuthor() != null) {
                        md.append(String.format("**By:** %s  \n", article.getAuthor()));
                    }

                    if (article.getPublishedDate() != null) {
                        md.append(String.format("**Date:** %s  \n",
                                article.getPublishedDate().toLocalDate()));
                    }

                    if (article.getReadingTimeMinutes() > 0) {
                        md.append(String.format("**Reading Time:** %d min  \n",
                                article.getReadingTimeMinutes()));
                    }

                    md.append(String.format("**Link:** [Read on Medium](%s)  \n\n", article.getUrl()));

                    if (article.getTldr() != null && !article.getTldr().isEmpty()) {
                        md.append(article.getTldr()).append("\n\n");
                    }

                    md.append("---\n\n");
                });

        FileUtils.writeStringToFile(new File(outputDir, "README.md"),
                md.toString(), StandardCharsets.UTF_8);
    }

    private void createCategoryPage(File outputDir, ArticleCategory category,
                                      List<MediumArticle> articles) throws IOException {
        StringBuilder md = new StringBuilder();

        md.append(String.format("# %s\n\n", category.getDisplayName()));
        md.append(String.format("**Total Articles:** %d\n\n", articles.size()));

        // Sort by date (newest first)
        List<MediumArticle> sorted = articles.stream()
                .sorted(Comparator.comparing(MediumArticle::getPublishedDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        for (MediumArticle article : sorted) {
            md.append(String.format("## [%s](./articles/%s.md)\n\n",
                    article.getTitle(),
                    sanitizeFilename(article.getId())));

            md.append(String.format("📅 %s",
                    article.getPublishedDate() != null ?
                            article.getPublishedDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) :
                            "Unknown"));

            if (article.getAuthor() != null) {
                md.append(String.format(" | ✍️ %s", article.getAuthor()));
            }

            if (article.getReadingTimeMinutes() > 0) {
                md.append(String.format(" | ⏱️ %d min", article.getReadingTimeMinutes()));
            }

            md.append("\n\n");

            if (article.getSummary() != null && !article.getSummary().isEmpty()) {
                md.append(article.getSummary()).append("\n\n");
            }

            md.append(String.format("🔗 [Read on Medium](%s)\n\n", article.getUrl()));

            md.append("---\n\n");
        }

        String filename = sanitizeFilename(category.name()) + ".md";
        FileUtils.writeStringToFile(new File(outputDir, filename),
                md.toString(), StandardCharsets.UTF_8);
    }

    private void createTimelinePage(File outputDir, List<MediumArticle> articles) throws IOException {
        StringBuilder md = new StringBuilder();

        md.append("# Article Timeline\n\n");
        md.append("All articles sorted by publication date (newest first)\n\n");

        // Group by month
        Map<String, List<MediumArticle>> byMonth = articles.stream()
                .filter(a -> a.getPublishedDate() != null)
                .sorted(Comparator.comparing(MediumArticle::getPublishedDate).reversed())
                .collect(Collectors.groupingBy(
                        a -> a.getPublishedDate().format(DateTimeFormatter.ofPattern("yyyy-MM")),
                        Collectors.toList()));

        for (Map.Entry<String, List<MediumArticle>> entry : byMonth.entrySet()) {
            String monthYear = entry.getKey();
            md.append(String.format("## %s (%d articles)\n\n",
                    monthYear, entry.getValue().size()));

            for (MediumArticle article : entry.getValue()) {
                md.append(String.format("- **[%s](./articles/%s.md)** - %s  \n",
                        article.getTitle(),
                        sanitizeFilename(article.getId()),
                        article.getPublishedDate().format(DateTimeFormatter.ofPattern("MMM dd"))));

                if (article.getAuthor() != null) {
                    md.append(String.format("  _By %s_  \n", article.getAuthor()));
                }

                if (!article.getTags().isEmpty()) {
                    md.append("  Tags: ").append(String.join(", ", article.getTags())).append("  \n");
                }

                md.append("\n");
            }
        }

        FileUtils.writeStringToFile(new File(outputDir, "timeline.md"),
                md.toString(), StandardCharsets.UTF_8);
    }

    private void createIndividualPages(File outputDir, List<MediumArticle> articles) throws IOException {
        File articlesDir = new File(outputDir, "articles");
        FileUtils.forceMkdir(articlesDir);

        for (MediumArticle article : articles) {
            StringBuilder md = new StringBuilder();

            // Header
            md.append("# ").append(article.getTitle()).append("\n\n");

            // Metadata
            md.append("| Property | Value |\n");
            md.append("|----------|-------|\n");
            md.append(String.format("| **Author** | %s |\n",
                    article.getAuthor() != null ? article.getAuthor() : "Unknown"));
            md.append(String.format("| **Published** | %s |\n",
                    article.getPublishedDate() != null ?
                            article.getPublishedDate().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")) :
                            "Unknown"));
            md.append(String.format("| **Category** | %s |\n",
                    article.getCategory() != null ? article.getCategory().getDisplayName() : "Unknown"));
            md.append(String.format("| **Reading Time** | %d min |\n", article.getReadingTimeMinutes()));
            md.append(String.format("| **Original Link** | [Read on Medium](%s) |\n", article.getUrl()));

            if (article.getClaps() > 0) {
                md.append(String.format("| **Claps** | %d |\n", article.getClaps()));
            }

            md.append("\n");

            // Tags
            if (!article.getTags().isEmpty()) {
                md.append("**Tags:** ");
                article.getTags().forEach(tag ->
                        md.append("`").append(tag).append("` "));
                md.append("\n\n");
            }

            // Subtitle
            if (article.getSubtitle() != null && !article.getSubtitle().isEmpty()) {
                md.append("## ").append(article.getSubtitle()).append("\n\n");
            }

            // TL;DR
            if (article.getTldr() != null && !article.getTldr().isEmpty()) {
                md.append("## TL;DR\n\n");
                md.append(article.getTldr()).append("\n\n");
            }

            // Summary
            if (article.getSummary() != null && !article.getSummary().isEmpty()) {
                md.append("## Summary\n\n");
                md.append(article.getSummary()).append("\n\n");
            }

            // Key Takeaways
            if (article.getKeyTakeaways() != null && !article.getKeyTakeaways().isEmpty()) {
                md.append("## 🔑 Key Takeaways\n\n");
                for (int i = 0; i < article.getKeyTakeaways().size(); i++) {
                    md.append(String.format("%d. %s\n", i + 1, article.getKeyTakeaways().get(i)));
                }
                md.append("\n");
            }

            // Topics
            if (article.getTopics() != null && !article.getTopics().isEmpty()) {
                md.append("## 📚 Topics Covered\n\n");
                article.getTopics().forEach(topic ->
                        md.append("- ").append(topic).append("\n"));
                md.append("\n");
            }

            // Full Content
            if (article.getFullText() != null && !article.getFullText().isEmpty()) {
                md.append("## Full Article\n\n");
                md.append(article.getFullText()).append("\n\n");
            }

            // Code Snippets
            if (article.getCodeSnippets() != null && !article.getCodeSnippets().isEmpty()) {
                md.append("## 💻 Code Examples\n\n");
                md.append(String.format("This article contains %d code examples.\n\n",
                        article.getCodeSnippets().size()));

                for (int i = 0; i < Math.min(3, article.getCodeSnippets().size()); i++) {
                    md.append("```kotlin\n");
                    md.append(article.getCodeSnippets().get(i));
                    md.append("\n```\n\n");
                }
            }

            // Footer
            md.append("---\n\n");
            md.append(String.format("📖 [Read full article on Medium](%s)\n", article.getUrl()));

            String filename = sanitizeFilename(article.getId()) + ".md";
            FileUtils.writeStringToFile(new File(articlesDir, filename),
                    md.toString(), StandardCharsets.UTF_8);
        }
    }

    private String sanitizeFilename(String name) {
        return name.replaceAll("[^a-zA-Z0-9.-]", "_");
    }
}
