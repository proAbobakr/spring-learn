package com.locationapp.service.medium.service;

import com.locationapp.service.medium.model.MediumArticle;
import com.locationapp.service.medium.model.MediumCrawlerConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for summarizing Medium articles
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleSummarizer {

    private final MediumCrawlerConfig config;

    /**
     * Enhance article with summaries and analysis
     */
    public void enhanceArticle(MediumArticle article) {
        if (article == null || article.getFullText() == null) return;

        try {
            // Generate summary
            if (config.isGenerateSummaries()) {
                article.setSummary(generateSummary(article));
            }

            // Generate TLDR
            if (config.isGenerateTldr()) {
                article.setTldr(generateTldr(article));
            }

            // Generate key takeaways
            if (config.isGenerateKeyTakeaways()) {
                article.setKeyTakeaways(extractKeyTakeaways(article));
            }

            // Extract topics
            article.setTopics(extractTopics(article));

            // Generate LLM context
            article.setLlmContext(generateLlmContext(article));

            log.debug("Enhanced article: {}", article.getTitle());

        } catch (Exception e) {
            log.error("Error enhancing article: {}", article.getTitle(), e);
        }
    }

    /**
     * Generate a comprehensive summary
     */
    private String generateSummary(MediumArticle article) {
        StringBuilder summary = new StringBuilder();

        String text = article.getFullText();
        if (text == null || text.isEmpty()) {
            return article.getSubtitle() != null ? article.getSubtitle() : "";
        }

        // Split into sentences
        String[] sentences = text.split("\\. ");

        // Take first few sentences and important ones
        List<String> importantSentences = new ArrayList<>();

        // Add first sentence (usually introduces topic)
        if (sentences.length > 0) {
            importantSentences.add(sentences[0]);
        }

        // Add sentences with key Android terms
        String[] keywords = {
                "Android", "Kotlin", "Compose", "ViewModel", "Fragment",
                "Activity", "Coroutine", "LiveData", "Room", "important",
                "key", "main", "essential"
        };

        for (String sentence : sentences) {
            if (importantSentences.size() >= 5) break;

            for (String keyword : keywords) {
                if (sentence.toLowerCase().contains(keyword.toLowerCase()) &&
                        !importantSentences.contains(sentence)) {
                    importantSentences.add(sentence);
                    break;
                }
            }
        }

        // Combine sentences
        String combined = String.join(". ", importantSentences);

        // Truncate if needed
        if (combined.length() > config.getSummaryMaxLength()) {
            combined = combined.substring(0, config.getSummaryMaxLength()) + "...";
        }

        return combined;
    }

    /**
     * Generate TLDR (Too Long; Didn't Read)
     */
    private String generateTldr(MediumArticle article) {
        StringBuilder tldr = new StringBuilder();

        tldr.append("📱 ").append(article.getTitle()).append("\n\n");

        if (article.getAuthor() != null) {
            tldr.append("✍️ By ").append(article.getAuthor()).append("\n");
        }

        if (article.getReadingTimeMinutes() > 0) {
            tldr.append("⏱️ ").append(article.getReadingTimeMinutes()).append(" min read\n");
        }

        if (article.getPublishedDate() != null) {
            tldr.append("📅 ").append(article.getPublishedDate().toLocalDate()).append("\n");
        }

        tldr.append("\n");

        // Add subtitle or excerpt
        if (article.getSubtitle() != null && !article.getSubtitle().isEmpty()) {
            tldr.append(article.getSubtitle());
        } else if (article.getExcerpt() != null) {
            tldr.append(article.getExcerpt());
        }

        return tldr.toString();
    }

    /**
     * Extract key takeaways from article
     */
    private List<String> extractKeyTakeaways(MediumArticle article) {
        List<String> takeaways = new ArrayList<>();

        String text = article.getFullText();
        if (text == null || text.isEmpty()) return takeaways;

        // Look for list items (often represent key points)
        String[] lines = text.split("\n");
        for (String line : lines) {
            line = line.trim();

            // Check if line starts with list markers
            if (line.matches("^[-*•]\\s+.+") || line.matches("^\\d+[.)]]\\s+.+")) {
                String takeaway = line.replaceFirst("^[-*•\\d+.)]\\s+", "");
                if (takeaway.length() > 20 && takeaway.length() < 200) {
                    takeaways.add(takeaway);
                }
            }
        }

        // If no list items found, extract sentences with key phrases
        if (takeaways.isEmpty()) {
            String[] keyPhrases = {
                    "should", "must", "important", "key", "remember",
                    "tip", "best practice", "recommendation", "avoid"
            };

            String[] sentences = text.split("\\. ");
            for (String sentence : sentences) {
                for (String phrase : keyPhrases) {
                    if (sentence.toLowerCase().contains(phrase) &&
                            sentence.length() > 30 && sentence.length() < 200) {
                        if (!takeaways.contains(sentence)) {
                            takeaways.add(sentence);
                        }
                        break;
                    }
                }
                if (takeaways.size() >= 5) break;
            }
        }

        // Limit to top 10
        return takeaways.stream().limit(10).collect(Collectors.toList());
    }

    /**
     * Extract main topics from article
     */
    private List<String> extractTopics(MediumArticle article) {
        List<String> topics = new ArrayList<>();

        // Add tags as topics
        if (article.getTags() != null) {
            topics.addAll(article.getTags());
        }

        // Extract topics from title and content
        String[] androidTopics = {
                "Jetpack Compose", "Kotlin", "Coroutines", "Flow", "ViewModel",
                "LiveData", "Room", "Hilt", "Dagger", "Navigation",
                "Material Design", "MVVM", "MVI", "Clean Architecture",
                "Testing", "UI", "Performance", "Security", "Gradle"
        };

        String searchText = (article.getTitle() + " " + article.getFullText()).toLowerCase();

        for (String topic : androidTopics) {
            if (searchText.contains(topic.toLowerCase()) && !topics.contains(topic)) {
                topics.add(topic);
            }
        }

        return topics.stream().distinct().limit(15).collect(Collectors.toList());
    }

    /**
     * Generate LLM-friendly context
     */
    private String generateLlmContext(MediumArticle article) {
        StringBuilder context = new StringBuilder();

        // Structured format for LLMs
        context.append("# ").append(article.getTitle()).append("\n\n");

        context.append("**Author:** ").append(article.getAuthor() != null ? article.getAuthor() : "Unknown")
                .append("\n");
        context.append("**Published:** ").append(article.getPublishedDate() != null ?
                article.getPublishedDate().toLocalDate() : "Unknown").append("\n");
        context.append("**Category:** ").append(article.getCategory() != null ?
                article.getCategory().getDisplayName() : "Unknown").append("\n");
        context.append("**Reading Time:** ").append(article.getReadingTimeMinutes())
                .append(" minutes\n");
        context.append("**URL:** ").append(article.getUrl()).append("\n\n");

        if (article.getSubtitle() != null && !article.getSubtitle().isEmpty()) {
            context.append("## Subtitle\n").append(article.getSubtitle()).append("\n\n");
        }

        if (article.getTldr() != null && !article.getTldr().isEmpty()) {
            context.append("## TL;DR\n").append(article.getTldr()).append("\n\n");
        }

        if (article.getSummary() != null && !article.getSummary().isEmpty()) {
            context.append("## Summary\n").append(article.getSummary()).append("\n\n");
        }

        if (article.getKeyTakeaways() != null && !article.getKeyTakeaways().isEmpty()) {
            context.append("## Key Takeaways\n");
            for (int i = 0; i < article.getKeyTakeaways().size(); i++) {
                context.append(i + 1).append(". ").append(article.getKeyTakeaways().get(i)).append("\n");
            }
            context.append("\n");
        }

        if (article.getTopics() != null && !article.getTopics().isEmpty()) {
            context.append("**Topics:** ").append(String.join(", ", article.getTopics())).append("\n\n");
        }

        if (article.getCodeSnippets() != null && !article.getCodeSnippets().isEmpty()) {
            context.append("## Code Examples\n");
            context.append("This article contains ").append(article.getCodeSnippets().size())
                    .append(" code examples.\n\n");
        }

        if (article.getFullText() != null && !article.getFullText().isEmpty()) {
            context.append("## Full Content\n").append(article.getFullText());
        }

        return context.toString();
    }
}
