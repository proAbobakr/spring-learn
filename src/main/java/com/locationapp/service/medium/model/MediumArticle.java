package com.locationapp.service.medium.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Medium article from ProAndroidDev
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MediumArticle {

    private String id;
    private String title;
    private String url;
    private String author;
    private String authorUrl;

    // Content
    private String subtitle;
    private String fullText;
    private String excerpt;

    // Metadata
    private LocalDateTime publishedDate;
    private LocalDateTime scrapedDate;
    private int readingTimeMinutes;
    private int claps;
    private int responses;

    // Categories and tags
    @Builder.Default
    private List<String> tags = new ArrayList<>();
    private ArticleCategory category;

    // Images
    private String featuredImage;
    @Builder.Default
    private List<String> images = new ArrayList<>();

    // Generated content
    private String summary;
    private String tldr;
    @Builder.Default
    private List<String> keyTakeaways = new ArrayList<>();
    @Builder.Default
    private List<String> topics = new ArrayList<>();
    @Builder.Default
    private List<String> codeSnippets = new ArrayList<>();

    // LLM-friendly content
    private String llmContext;

    /**
     * Check if article is within date range
     */
    public boolean isAfterDate(LocalDateTime date) {
        return publishedDate != null && publishedDate.isAfter(date);
    }
}
