package com.locationapp.service.medium.model;

/**
 * Categories for Android development articles
 */
public enum ArticleCategory {
    JETPACK_COMPOSE("Jetpack Compose", "compose, declarative ui"),
    KOTLIN("Kotlin", "kotlin, coroutines, flow"),
    ARCHITECTURE("Architecture", "mvvm, mvi, clean architecture"),
    UI_UX("UI/UX", "material design, animations, ui"),
    TESTING("Testing", "unit test, ui test, testing"),
    PERFORMANCE("Performance", "optimization, memory, performance"),
    SECURITY("Security", "security, encryption, authentication"),
    LIBRARIES("Libraries & Tools", "library, gradle, dependency"),
    BEST_PRACTICES("Best Practices", "best practice, tips, patterns"),
    TUTORIALS("Tutorials", "tutorial, guide, how to"),
    NEWS("News & Updates", "news, update, release"),
    CAREER("Career & Development", "career, interview, learning"),
    OTHER("Other", "");

    private final String displayName;
    private final String keywords;

    ArticleCategory(String displayName, String keywords) {
        this.displayName = displayName;
        this.keywords = keywords;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getKeywords() {
        return keywords;
    }

    /**
     * Categorize article based on tags and content
     */
    public static ArticleCategory categorize(List<String> tags, String title, String content) {
        String searchText = (String.join(" ", tags) + " " + title + " " + content).toLowerCase();

        for (ArticleCategory category : values()) {
            if (category == OTHER) continue;

            String[] keywords = category.keywords.split(",\\s*");
            for (String keyword : keywords) {
                if (searchText.contains(keyword.toLowerCase())) {
                    return category;
                }
            }
        }

        return OTHER;
    }
}
