package com.locationapp.service.medium.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.locationapp.service.medium.model.MediumArticle;
import com.locationapp.service.medium.model.MediumCrawlerConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Exports Medium articles in LLM-friendly formats
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MediumLLMExporter {

    private final MediumCrawlerConfig config;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public void export(List<MediumArticle> articles) {
        try {
            File outputDir = new File(config.getOutputDirectory(), "llm-format");
            FileUtils.forceMkdir(outputDir);

            log.info("Exporting LLM formats: {}", outputDir.getAbsolutePath());

            // Export as JSONL
            exportAsJsonLines(outputDir, articles);

            // Export as JSON by category
            exportByCategoryJson(outputDir, articles);

            // Export as embeddings-ready format
            exportAsEmbeddingsFormat(outputDir, articles);

            // Export metadata
            exportMetadata(outputDir, articles);

            log.info("LLM export completed");

        } catch (IOException e) {
            log.error("Error exporting LLM formats", e);
        }
    }

    private void exportAsJsonLines(File outputDir, List<MediumArticle> articles)
            throws IOException {
        File jsonlFile = new File(outputDir, "proandroiddev-articles.jsonl");

        try (FileWriter writer = new FileWriter(jsonlFile, StandardCharsets.UTF_8)) {
            for (MediumArticle article : articles) {
                LLMDocument doc = createLLMDocument(article);
                String json = objectMapper.writeValueAsString(doc);
                writer.write(json + "\n");
            }
        }

        log.info("Created JSONL file: {} ({} articles)",
                jsonlFile.getName(), articles.size());
    }

    private void exportByCategoryJson(File outputDir, List<MediumArticle> articles)
            throws IOException {
        File categoriesDir = new File(outputDir, "by-category");
        FileUtils.forceMkdir(categoriesDir);

        Map<String, List<MediumArticle>> byCategory = new HashMap<>();
        for (MediumArticle article : articles) {
            String category = article.getCategory() != null ?
                    article.getCategory().name() : "OTHER";
            byCategory.computeIfAbsent(category, k -> new ArrayList<>()).add(article);
        }

        for (Map.Entry<String, List<MediumArticle>> entry : byCategory.entrySet()) {
            List<LLMDocument> docs = entry.getValue().stream()
                    .map(this::createLLMDocument)
                    .toList();

            String filename = entry.getKey().toLowerCase() + ".json";
            File categoryFile = new File(categoriesDir, filename);

            objectMapper.writeValue(categoryFile, docs);
            log.debug("Created category file: {} ({} articles)",
                    filename, docs.size());
        }
    }

    private void exportAsEmbeddingsFormat(File outputDir, List<MediumArticle> articles)
            throws IOException {
        File embeddingsFile = new File(outputDir, "embeddings-ready.jsonl");

        try (FileWriter writer = new FileWriter(embeddingsFile, StandardCharsets.UTF_8)) {
            for (MediumArticle article : articles) {
                List<EmbeddingChunk> chunks = createEmbeddingChunks(article);
                for (EmbeddingChunk chunk : chunks) {
                    String json = objectMapper.writeValueAsString(chunk);
                    writer.write(json + "\n");
                }
            }
        }

        log.info("Created embeddings file: {}", embeddingsFile.getName());
    }

    private void exportMetadata(File outputDir, List<MediumArticle> articles)
            throws IOException {
        Map<String, Object> metadata = new HashMap<>();

        metadata.put("export_date", new Date().toString());
        metadata.put("publication", config.getPublicationName());
        metadata.put("total_articles", articles.size());
        metadata.put("date_filter", config.getFilterAfterDate());

        // Category breakdown
        Map<String, Long> categoryStats = new HashMap<>();
        articles.stream()
                .filter(a -> a.getCategory() != null)
                .forEach(a -> categoryStats.merge(
                        a.getCategory().getDisplayName(), 1L, Long::sum));
        metadata.put("category_breakdown", categoryStats);

        // Date range
        articles.stream()
                .filter(a -> a.getPublishedDate() != null)
                .min(Comparator.comparing(MediumArticle::getPublishedDate))
                .ifPresent(a -> metadata.put("oldest_article",
                        a.getPublishedDate().toLocalDate().toString()));

        articles.stream()
                .filter(a -> a.getPublishedDate() != null)
                .max(Comparator.comparing(MediumArticle::getPublishedDate))
                .ifPresent(a -> metadata.put("newest_article",
                        a.getPublishedDate().toLocalDate().toString()));

        // Schema
        Map<String, Object> schema = new HashMap<>();
        schema.put("document_fields", Arrays.asList(
                "id", "title", "url", "author", "published_date", "category",
                "summary", "tldr", "key_takeaways", "topics", "full_text",
                "tags", "reading_time", "claps"
        ));
        metadata.put("schema", schema);

        File metadataFile = new File(outputDir, "metadata.json");
        objectMapper.writeValue(metadataFile, metadata);
    }

    private LLMDocument createLLMDocument(MediumArticle article) {
        LLMDocument doc = new LLMDocument();

        doc.setId(article.getId());
        doc.setTitle(article.getTitle());
        doc.setUrl(article.getUrl());
        doc.setAuthor(article.getAuthor());
        doc.setPublishedDate(article.getPublishedDate() != null ?
                article.getPublishedDate().format(DateTimeFormatter.ISO_LOCAL_DATE) : null);
        doc.setCategory(article.getCategory() != null ?
                article.getCategory().getDisplayName() : null);

        doc.setSummary(article.getSummary());
        doc.setTldr(article.getTldr());
        doc.setKeyTakeaways(article.getKeyTakeaways());
        doc.setTopics(article.getTopics());
        doc.setTags(article.getTags());

        doc.setFullText(article.getFullText());
        doc.setReadingTimeMinutes(article.getReadingTimeMinutes());
        doc.setClaps(article.getClaps());

        // Combined content for search
        StringBuilder content = new StringBuilder();
        if (article.getTitle() != null) content.append(article.getTitle()).append(" ");
        if (article.getSummary() != null) content.append(article.getSummary()).append(" ");
        if (article.getTldr() != null) content.append(article.getTldr()).append(" ");
        doc.setContent(content.toString().trim());

        // LLM context
        doc.setLlmContext(article.getLlmContext());

        return doc;
    }

    private List<EmbeddingChunk> createEmbeddingChunks(MediumArticle article) {
        List<EmbeddingChunk> chunks = new ArrayList<>();

        Map<String, String> metadata = new HashMap<>();
        metadata.put("title", article.getTitle());
        metadata.put("author", article.getAuthor() != null ? article.getAuthor() : "Unknown");
        metadata.put("category", article.getCategory() != null ?
                article.getCategory().getDisplayName() : "Unknown");
        metadata.put("url", article.getUrl());

        // Overview chunk
        EmbeddingChunk overview = new EmbeddingChunk();
        overview.setId(article.getId() + "-overview");
        overview.setArticleId(article.getId());
        overview.setChunkType("overview");
        overview.setText(String.format("%s by %s: %s",
                article.getTitle(),
                article.getAuthor() != null ? article.getAuthor() : "Unknown",
                article.getSummary() != null ? article.getSummary() : ""));
        overview.setMetadata(metadata);
        chunks.add(overview);

        // TL;DR chunk
        if (article.getTldr() != null && !article.getTldr().isEmpty()) {
            EmbeddingChunk tldr = new EmbeddingChunk();
            tldr.setId(article.getId() + "-tldr");
            tldr.setArticleId(article.getId());
            tldr.setChunkType("tldr");
            tldr.setText(article.getTldr());
            tldr.setMetadata(metadata);
            chunks.add(tldr);
        }

        // Key takeaways chunk
        if (article.getKeyTakeaways() != null && !article.getKeyTakeaways().isEmpty()) {
            EmbeddingChunk takeaways = new EmbeddingChunk();
            takeaways.setId(article.getId() + "-takeaways");
            takeaways.setArticleId(article.getId());
            takeaways.setChunkType("key_takeaways");
            takeaways.setText("Key takeaways: " + String.join(". ", article.getKeyTakeaways()));
            takeaways.setMetadata(metadata);
            chunks.add(takeaways);
        }

        // Full text chunks (split into smaller parts if too long)
        if (article.getFullText() != null && !article.getFullText().isEmpty()) {
            String fullText = article.getFullText();
            int chunkSize = 1000; // ~1000 characters per chunk

            if (fullText.length() > chunkSize) {
                String[] paragraphs = fullText.split("\n\n");
                StringBuilder currentChunk = new StringBuilder();
                int chunkIndex = 0;

                for (String paragraph : paragraphs) {
                    if (currentChunk.length() + paragraph.length() > chunkSize) {
                        // Save current chunk
                        if (currentChunk.length() > 0) {
                            EmbeddingChunk textChunk = new EmbeddingChunk();
                            textChunk.setId(article.getId() + "-text-" + chunkIndex);
                            textChunk.setArticleId(article.getId());
                            textChunk.setChunkType("content");
                            textChunk.setText(currentChunk.toString());
                            textChunk.setMetadata(metadata);
                            chunks.add(textChunk);

                            chunkIndex++;
                            currentChunk = new StringBuilder();
                        }
                    }
                    currentChunk.append(paragraph).append("\n\n");
                }

                // Add remaining chunk
                if (currentChunk.length() > 0) {
                    EmbeddingChunk textChunk = new EmbeddingChunk();
                    textChunk.setId(article.getId() + "-text-" + chunkIndex);
                    textChunk.setArticleId(article.getId());
                    textChunk.setChunkType("content");
                    textChunk.setText(currentChunk.toString());
                    textChunk.setMetadata(metadata);
                    chunks.add(textChunk);
                }
            } else {
                EmbeddingChunk textChunk = new EmbeddingChunk();
                textChunk.setId(article.getId() + "-text");
                textChunk.setArticleId(article.getId());
                textChunk.setChunkType("content");
                textChunk.setText(fullText);
                textChunk.setMetadata(metadata);
                chunks.add(textChunk);
            }
        }

        return chunks;
    }

    @lombok.Data
    public static class LLMDocument {
        private String id;
        private String title;
        private String url;
        private String author;
        private String publishedDate;
        private String category;

        private String summary;
        private String tldr;
        private List<String> keyTakeaways;
        private List<String> topics;
        private List<String> tags;

        private String fullText;
        private String content; // Combined searchable content

        private int readingTimeMinutes;
        private int claps;

        private String llmContext;
    }

    @lombok.Data
    public static class EmbeddingChunk {
        private String id;
        private String articleId;
        private String chunkType;
        private String text;
        private Map<String, String> metadata;
    }
}
