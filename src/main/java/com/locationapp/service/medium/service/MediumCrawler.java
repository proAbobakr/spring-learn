package com.locationapp.service.medium.service;

import com.locationapp.service.medium.model.ArticleCategory;
import com.locationapp.service.medium.model.CrawlStats;
import com.locationapp.service.medium.model.MediumArticle;
import com.locationapp.service.medium.model.MediumCrawlerConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Crawler for ProAndroidDev Medium publication
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MediumCrawler {

    private final MediumCrawlerConfig config;
    private final Set<String> visitedUrls = ConcurrentHashMap.newKeySet();
    private final List<MediumArticle> articles = new ArrayList<>();

    /**
     * Crawl ProAndroidDev articles
     */
    public List<MediumArticle> crawl() {
        log.info("Starting ProAndroidDev Medium crawl");
        long startTime = System.currentTimeMillis();

        try {
            // Try multiple approaches to get articles
            crawlPublicationArchive();
            crawlPublicationLatest();
            crawlPublicationFeed();

            // Filter by date if configured
            List<MediumArticle> filtered = filterArticles();

            log.info("Crawl completed. Found {} articles ({} after date filter)",
                    articles.size(), filtered.size());

            return filtered;

        } catch (Exception e) {
            log.error("Error during Medium crawl", e);
            return Collections.emptyList();
        }
    }

    /**
     * Crawl publication archive page
     */
    private void crawlPublicationArchive() {
        String archiveUrl = config.getPublicationUrl() + "/archive";
        log.info("Crawling archive: {}", archiveUrl);

        try {
            Document doc = fetchDocument(archiveUrl);
            if (doc == null) return;

            // Find article links
            Elements articleElements = doc.select("article, .streamItem, .postArticle");

            for (Element articleElement : articleElements) {
                if (articles.size() >= config.getMaxArticles()) break;

                try {
                    MediumArticle article = parseArticleElement(articleElement);
                    if (article != null && !isDuplicate(article)) {
                        articles.add(article);
                        log.debug("Found article: {}", article.getTitle());
                    }
                } catch (Exception e) {
                    log.warn("Error parsing article element", e);
                }

                sleep(config.getRequestDelayMs());
            }

        } catch (Exception e) {
            log.warn("Error crawling archive: {}", e.getMessage());
        }
    }

    /**
     * Crawl latest articles
     */
    private void crawlPublicationLatest() {
        String latestUrl = config.getPublicationUrl() + "/latest";
        log.info("Crawling latest: {}", latestUrl);

        try {
            Document doc = fetchDocument(latestUrl);
            if (doc == null) return;

            Elements articleLinks = doc.select("a[href*='/p/'], a[data-action='open-post']");

            for (Element link : articleLinks) {
                if (articles.size() >= config.getMaxArticles()) break;

                String articleUrl = link.attr("abs:href");
                if (articleUrl.isEmpty() || visitedUrls.contains(articleUrl)) continue;

                MediumArticle article = crawlArticlePage(articleUrl);
                if (article != null && !isDuplicate(article)) {
                    articles.add(article);
                }

                sleep(config.getRequestDelayMs());
            }

        } catch (Exception e) {
            log.warn("Error crawling latest: {}", e.getMessage());
        }
    }

    /**
     * Crawl RSS/Atom feed
     */
    private void crawlPublicationFeed() {
        String feedUrl = config.getPublicationUrl() + "/feed";
        log.info("Crawling feed: {}", feedUrl);

        try {
            Document doc = fetchDocument(feedUrl);
            if (doc == null) return;

            Elements items = doc.select("item, entry");

            for (Element item : items) {
                if (articles.size() >= config.getMaxArticles()) break;

                try {
                    MediumArticle article = parseRssItem(item);
                    if (article != null && !isDuplicate(article)) {
                        articles.add(article);
                    }
                } catch (Exception e) {
                    log.warn("Error parsing RSS item", e);
                }
            }

        } catch (Exception e) {
            log.warn("Error crawling feed: {}", e.getMessage());
        }
    }

    /**
     * Crawl individual article page
     */
    private MediumArticle crawlArticlePage(String url) {
        if (visitedUrls.contains(url)) return null;

        try {
            log.debug("Crawling article: {}", url);
            Document doc = fetchDocument(url);
            if (doc == null) return null;

            visitedUrls.add(url);

            return parseArticlePage(doc, url);

        } catch (Exception e) {
            log.warn("Error crawling article {}: {}", url, e.getMessage());
            return null;
        }
    }

    /**
     * Parse article from its dedicated page
     */
    private MediumArticle parseArticlePage(Document doc, String url) {
        try {
            MediumArticle.MediumArticleBuilder builder = MediumArticle.builder();

            // Extract title
            Element titleElement = doc.selectFirst("h1, article h1, [data-testid='storyTitle']");
            String title = titleElement != null ? titleElement.text() : extractTitleFromMeta(doc);
            builder.title(title);
            builder.url(url);

            // Extract author
            Element authorElement = doc.selectFirst("a[rel='author'], [data-testid='authorName']");
            if (authorElement != null) {
                builder.author(authorElement.text());
                builder.authorUrl(authorElement.attr("abs:href"));
            }

            // Extract subtitle
            Element subtitleElement = doc.selectFirst("h2, article h2, .subtitle");
            if (subtitleElement != null) {
                builder.subtitle(subtitleElement.text());
            }

            // Extract published date
            LocalDateTime publishDate = extractPublishedDate(doc);
            builder.publishedDate(publishDate);
            builder.scrapedDate(LocalDateTime.now());

            // Extract content
            String fullText = extractArticleContent(doc);
            builder.fullText(fullText);
            builder.excerpt(generateExcerpt(fullText));

            // Extract metadata
            builder.readingTimeMinutes(extractReadingTime(doc));
            builder.claps(extractClaps(doc));
            builder.responses(extractResponses(doc));

            // Extract tags
            List<String> tags = extractTags(doc);
            builder.tags(tags);

            // Extract images
            if (config.isExtractImages()) {
                builder.featuredImage(extractFeaturedImage(doc));
                builder.images(extractImages(doc));
            }

            // Extract code snippets
            if (config.isExtractCodeSnippets()) {
                builder.codeSnippets(extractCodeSnippets(doc));
            }

            // Categorize
            ArticleCategory category = ArticleCategory.categorize(tags, title, fullText);
            builder.category(category);

            // Generate ID
            builder.id(generateArticleId(url));

            return builder.build();

        } catch (Exception e) {
            log.error("Error parsing article page: {}", url, e);
            return null;
        }
    }

    /**
     * Parse article from list element
     */
    private MediumArticle parseArticleElement(Element element) {
        try {
            // Find title link
            Element titleLink = element.selectFirst("h3 a, h2 a, a[data-action='open-post']");
            if (titleLink == null) return null;

            String url = titleLink.attr("abs:href");
            if (url.isEmpty() || visitedUrls.contains(url)) return null;

            // For list elements, we might need to fetch the full page
            return crawlArticlePage(url);

        } catch (Exception e) {
            log.warn("Error parsing article element", e);
            return null;
        }
    }

    /**
     * Parse article from RSS/Atom item
     */
    private MediumArticle parseRssItem(Element item) {
        try {
            MediumArticle.MediumArticleBuilder builder = MediumArticle.builder();

            // Title
            String title = item.selectFirst("title") != null ?
                    item.selectFirst("title").text() : "";
            builder.title(title);

            // Link
            Element linkElement = item.selectFirst("link");
            String url = linkElement != null ? linkElement.text() : "";
            if (url.isEmpty()) {
                url = linkElement != null ? linkElement.attr("href") : "";
            }
            builder.url(url);
            builder.id(generateArticleId(url));

            // Author
            Element authorElement = item.selectFirst("creator, author name, dc\\:creator");
            if (authorElement != null) {
                builder.author(authorElement.text());
            }

            // Published date
            Element pubDateElement = item.selectFirst("pubDate, published, dc\\:date");
            if (pubDateElement != null) {
                try {
                    LocalDateTime pubDate = parseRssDate(pubDateElement.text());
                    builder.publishedDate(pubDate);
                } catch (Exception e) {
                    log.debug("Could not parse date: {}", pubDateElement.text());
                }
            }

            // Description/Content
            Element descElement = item.selectFirst("description, content\\:encoded, summary");
            if (descElement != null) {
                String content = descElement.text();
                builder.fullText(content);
                builder.excerpt(generateExcerpt(content));
            }

            // Categories/Tags
            Elements categories = item.select("category");
            List<String> tags = new ArrayList<>();
            for (Element category : categories) {
                tags.add(category.text());
            }
            builder.tags(tags);

            // Categorize
            ArticleCategory category = ArticleCategory.categorize(tags, title,
                    builder.build().getFullText() != null ? builder.build().getFullText() : "");
            builder.category(category);

            builder.scrapedDate(LocalDateTime.now());

            return builder.build();

        } catch (Exception e) {
            log.warn("Error parsing RSS item", e);
            return null;
        }
    }

    /**
     * Extract article content from page
     */
    private String extractArticleContent(Document doc) {
        StringBuilder content = new StringBuilder();

        // Try different selectors for article content
        String[] selectors = {
                "article section",
                "article .section-content",
                "[data-testid='storyContent']",
                ".postArticle-content",
                ".article-body"
        };

        for (String selector : selectors) {
            Elements contentElements = doc.select(selector + " p, " + selector + " h1, " +
                    selector + " h2, " + selector + " h3, " + selector + " li");

            if (!contentElements.isEmpty()) {
                for (Element p : contentElements) {
                    content.append(p.text()).append("\n\n");
                }
                break;
            }
        }

        return content.toString().trim();
    }

    private LocalDateTime extractPublishedDate(Document doc) {
        // Try meta tags first
        Element metaDate = doc.selectFirst("meta[property='article:published_time']");
        if (metaDate != null) {
            return parseIsoDate(metaDate.attr("content"));
        }

        // Try time element
        Element timeElement = doc.selectFirst("time[datetime], [data-testid='storyPublishDate']");
        if (timeElement != null) {
            String datetime = timeElement.attr("datetime");
            if (!datetime.isEmpty()) {
                return parseIsoDate(datetime);
            }
        }

        return LocalDateTime.now();
    }

    private int extractReadingTime(Document doc) {
        Element readingTime = doc.selectFirst("[data-testid='storyReadTime'], .readingTime");
        if (readingTime != null) {
            String text = readingTime.text();
            Pattern pattern = Pattern.compile("(\\d+)");
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1));
            }
        }
        return 0;
    }

    private int extractClaps(Document doc) {
        Element clapsElement = doc.selectFirst("button[data-action='show-recommends']");
        if (clapsElement != null) {
            String text = clapsElement.text();
            return extractNumber(text);
        }
        return 0;
    }

    private int extractResponses(Document doc) {
        Element responsesElement = doc.selectFirst("a[href*='responses']");
        if (responsesElement != null) {
            String text = responsesElement.text();
            return extractNumber(text);
        }
        return 0;
    }

    private List<String> extractTags(Document doc) {
        List<String> tags = new ArrayList<>();
        Elements tagElements = doc.select("a[href*='/tag/'], ul li a");

        for (Element tag : tagElements) {
            String tagText = tag.text().trim();
            if (!tagText.isEmpty() && tagText.length() < 50) {
                tags.add(tagText);
            }
        }

        return tags;
    }

    private String extractFeaturedImage(Document doc) {
        Element img = doc.selectFirst("article img, figure img, [data-testid='storyImage'] img");
        return img != null ? img.attr("abs:src") : null;
    }

    private List<String> extractImages(Document doc) {
        List<String> images = new ArrayList<>();
        Elements imgElements = doc.select("article img, figure img");

        for (Element img : imgElements) {
            String src = img.attr("abs:src");
            if (!src.isEmpty() && !images.contains(src)) {
                images.add(src);
            }
        }

        return images;
    }

    private List<String> extractCodeSnippets(Document doc) {
        List<String> snippets = new ArrayList<>();
        Elements codeBlocks = doc.select("pre, code, .gist");

        for (Element code : codeBlocks) {
            String snippet = code.text();
            if (snippet.length() > 20) { // Only substantial code blocks
                snippets.add(snippet);
            }
        }

        return snippets;
    }

    private String extractTitleFromMeta(Document doc) {
        Element metaTitle = doc.selectFirst("meta[property='og:title'], meta[name='twitter:title']");
        return metaTitle != null ? metaTitle.attr("content") : "Untitled";
    }

    private String generateExcerpt(String text) {
        if (text == null || text.isEmpty()) return "";
        int maxLength = 300;
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }

    private String generateArticleId(String url) {
        // Extract article ID from URL
        Pattern pattern = Pattern.compile("/([a-f0-9]+)$|/p/([a-z0-9-]+)");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
        }
        return String.valueOf(url.hashCode());
    }

    private int extractNumber(String text) {
        Pattern pattern = Pattern.compile("(\\d+(?:,\\d+)*)");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1).replace(",", ""));
        }
        return 0;
    }

    private LocalDateTime parseIsoDate(String dateStr) {
        try {
            return ZonedDateTime.parse(dateStr).toLocalDateTime();
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }

    private LocalDateTime parseRssDate(String dateStr) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.RFC_1123_DATE_TIME;
            return ZonedDateTime.parse(dateStr, formatter).toLocalDateTime();
        } catch (Exception e) {
            return parseIsoDate(dateStr);
        }
    }

    private boolean isDuplicate(MediumArticle article) {
        return articles.stream()
                .anyMatch(a -> a.getUrl().equals(article.getUrl()) ||
                        a.getTitle().equals(article.getTitle()));
    }

    private List<MediumArticle> filterArticles() {
        if (!config.isOnlyRecentArticles()) {
            return new ArrayList<>(articles);
        }

        LocalDateTime filterDate = config.getFilterDate();
        return articles.stream()
                .filter(a -> a.getPublishedDate() != null &&
                        a.getPublishedDate().isAfter(filterDate))
                .toList();
    }

    private Document fetchDocument(String url) {
        for (int retry = 0; retry < config.getMaxRetries(); retry++) {
            try {
                log.debug("Fetching: {} (attempt {})", url, retry + 1);

                Document doc = Jsoup.connect(url)
                        .userAgent(config.getUserAgent())
                        .timeout(config.getTimeoutSeconds() * 1000)
                        .followRedirects(true)
                        .get();

                return doc;

            } catch (IOException e) {
                log.warn("Failed to fetch {} (attempt {}): {}",
                        url, retry + 1, e.getMessage());

                if (retry < config.getMaxRetries() - 1) {
                    sleep(config.getRequestDelayMs() * (retry + 1));
                }
            }
        }

        return null;
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public List<MediumArticle> getArticles() {
        return new ArrayList<>(articles);
    }
}
