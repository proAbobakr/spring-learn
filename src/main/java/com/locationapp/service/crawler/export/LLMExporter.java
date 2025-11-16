package com.locationapp.service.crawler.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.locationapp.service.crawler.config.CrawlerConfig;
import com.locationapp.service.crawler.model.AndroidCategory;
import com.locationapp.service.crawler.model.AndroidDocElement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Exports documentation in LLM-friendly formats (JSON, JSONL)
 * Optimized for feeding into local LLMs and RAG systems
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LLMExporter {

    private final CrawlerConfig config;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    /**
     * Export documentation in LLM-friendly formats
     */
    public void export(Map<AndroidCategory, List<AndroidDocElement>> categorizedDocs) {
        try {
            File outputDir = new File(config.getOutputDirectory(), "llm-format");
            FileUtils.forceMkdir(outputDir);

            log.info("Exporting LLM-friendly formats: {}", outputDir.getAbsolutePath());

            // Export as JSONL (one JSON object per line) - best for RAG
            exportAsJsonLines(outputDir, categorizedDocs);

            // Export as structured JSON by category
            exportAsCategorizedJson(outputDir, categorizedDocs);

            // Export as embeddings-ready format
            exportAsEmbeddingsFormat(outputDir, categorizedDocs);

            // Export metadata
            exportMetadata(outputDir, categorizedDocs);

            log.info("LLM format export completed successfully");

        } catch (IOException e) {
            log.error("Error exporting LLM formats", e);
        }
    }

    /**
     * Export as JSONL (JSON Lines) format
     * Each line is a complete, self-contained JSON document
     * Perfect for streaming and RAG systems
     */
    private void exportAsJsonLines(File outputDir, Map<AndroidCategory, List<AndroidDocElement>> docs)
            throws IOException {
        File jsonlFile = new File(outputDir, "android-docs.jsonl");

        try (FileWriter writer = new FileWriter(jsonlFile, StandardCharsets.UTF_8)) {
            for (Map.Entry<AndroidCategory, List<AndroidDocElement>> entry : docs.entrySet()) {
                for (AndroidDocElement element : entry.getValue()) {
                    LLMDocument llmDoc = createLLMDocument(element);
                    String json = objectMapper.writeValueAsString(llmDoc);
                    writer.write(json + "\n");
                }
            }
        }

        log.info("Created JSONL file: {} ({} lines)",
                jsonlFile.getName(),
                docs.values().stream().mapToInt(List::size).sum());
    }

    /**
     * Export as categorized JSON files (one file per category)
     */
    private void exportAsCategorizedJson(File outputDir, Map<AndroidCategory, List<AndroidDocElement>> docs)
            throws IOException {
        File categorizedDir = new File(outputDir, "by-category");
        FileUtils.forceMkdir(categorizedDir);

        for (Map.Entry<AndroidCategory, List<AndroidDocElement>> entry : docs.entrySet()) {
            AndroidCategory category = entry.getKey();
            List<AndroidDocElement> elements = entry.getValue();

            List<LLMDocument> llmDocs = elements.stream()
                    .map(this::createLLMDocument)
                    .toList();

            String filename = category.name().toLowerCase() + ".json";
            File categoryFile = new File(categorizedDir, filename);

            objectMapper.writeValue(categoryFile, llmDocs);

            log.debug("Created category file: {} ({} documents)",
                    filename, llmDocs.size());
        }
    }

    /**
     * Export in embeddings-ready format
     * Optimized text chunks for vector embeddings
     */
    private void exportAsEmbeddingsFormat(File outputDir, Map<AndroidCategory, List<AndroidDocElement>> docs)
            throws IOException {
        File embeddingsFile = new File(outputDir, "embeddings-ready.jsonl");
        List<EmbeddingDocument> embeddingDocs = new ArrayList<>();

        try (FileWriter writer = new FileWriter(embeddingsFile, StandardCharsets.UTF_8)) {
            for (Map.Entry<AndroidCategory, List<AndroidDocElement>> entry : docs.entrySet()) {
                for (AndroidDocElement element : entry.getValue()) {
                    // Create multiple embedding documents for different aspects
                    embeddingDocs.addAll(createEmbeddingDocuments(element));
                }
            }

            // Write all embedding documents
            for (EmbeddingDocument doc : embeddingDocs) {
                String json = objectMapper.writeValueAsString(doc);
                writer.write(json + "\n");
            }
        }

        log.info("Created embeddings file: {} ({} chunks)",
                embeddingsFile.getName(), embeddingDocs.size());
    }

    /**
     * Export metadata about the documentation
     */
    private void exportMetadata(File outputDir, Map<AndroidCategory, List<AndroidDocElement>> docs)
            throws IOException {
        Map<String, Object> metadata = new HashMap<>();

        metadata.put("export_date", new Date().toString());
        metadata.put("total_documents", docs.values().stream().mapToInt(List::size).sum());
        metadata.put("total_categories", docs.size());

        Map<String, Integer> categoryStats = new HashMap<>();
        docs.forEach((category, elements) ->
                categoryStats.put(category.getDisplayName(), elements.size()));
        metadata.put("category_breakdown", categoryStats);

        Map<String, Object> schema = new HashMap<>();
        schema.put("document_fields", Arrays.asList(
                "id", "name", "type", "category", "package", "summary",
                "description", "kotlin_example", "java_example", "use_cases",
                "best_practices", "related_apis", "url"
        ));
        metadata.put("schema", schema);

        File metadataFile = new File(outputDir, "metadata.json");
        objectMapper.writeValue(metadataFile, metadata);
    }

    /**
     * Create LLM-optimized document from Android doc element
     */
    private LLMDocument createLLMDocument(AndroidDocElement element) {
        LLMDocument doc = new LLMDocument();

        doc.setId(generateId(element));
        doc.setName(element.getName());
        doc.setFullName(element.getFullName());
        doc.setType(element.getType().getDisplayName());
        doc.setCategory(element.getCategory().getDisplayName());
        doc.setPackageName(element.getPackageName());

        // Combine summary and description for better context
        StringBuilder content = new StringBuilder();
        if (element.getSummary() != null) {
            content.append(element.getSummary()).append("\n\n");
        }
        if (element.getDescription() != null && !element.getDescription().equals(element.getSummary())) {
            content.append(element.getDescription()).append("\n\n");
        }
        if (element.getAiExplanation() != null) {
            content.append(element.getAiExplanation());
        }
        doc.setContent(content.toString().trim());

        doc.setSummary(element.getSummary());
        doc.setDescription(element.getDescription());
        doc.setKotlinExample(element.getKotlinExample());
        doc.setJavaExample(element.getJavaExample());
        doc.setUseCases(element.getUseCases());
        doc.setBestPractices(element.getBestPractices());
        doc.setRelatedApis(element.getRelatedAPIs());
        doc.setUrl(element.getUrl());

        // Add methods summary
        if (element.getMethods() != null && !element.getMethods().isEmpty()) {
            List<String> methodNames = element.getMethods().stream()
                    .limit(10)
                    .map(AndroidDocElement::getName)
                    .toList();
            doc.setKeyMethods(methodNames);
        }

        return doc;
    }

    /**
     * Create multiple embedding documents for different aspects
     * This allows for more granular semantic search
     */
    private List<EmbeddingDocument> createEmbeddingDocuments(AndroidDocElement element) {
        List<EmbeddingDocument> docs = new ArrayList<>();
        String baseId = generateId(element);

        // 1. Overview chunk
        EmbeddingDocument overview = new EmbeddingDocument();
        overview.setId(baseId + "-overview");
        overview.setDocumentId(baseId);
        overview.setChunkType("overview");
        overview.setText(String.format("%s (%s): %s",
                element.getName(),
                element.getType().getDisplayName(),
                element.getSummary() != null ? element.getSummary() : ""));
        overview.setMetadata(createMetadata(element));
        docs.add(overview);

        // 2. Kotlin example chunk
        if (element.getKotlinExample() != null && !element.getKotlinExample().isEmpty()) {
            EmbeddingDocument kotlinChunk = new EmbeddingDocument();
            kotlinChunk.setId(baseId + "-kotlin");
            kotlinChunk.setDocumentId(baseId);
            kotlinChunk.setChunkType("kotlin_example");
            kotlinChunk.setText(String.format("Kotlin example for %s:\n%s",
                    element.getName(), element.getKotlinExample()));
            kotlinChunk.setMetadata(createMetadata(element));
            docs.add(kotlinChunk);
        }

        // 3. Java example chunk
        if (element.getJavaExample() != null && !element.getJavaExample().isEmpty()) {
            EmbeddingDocument javaChunk = new EmbeddingDocument();
            javaChunk.setId(baseId + "-java");
            javaChunk.setDocumentId(baseId);
            javaChunk.setChunkType("java_example");
            javaChunk.setText(String.format("Java example for %s:\n%s",
                    element.getName(), element.getJavaExample()));
            javaChunk.setMetadata(createMetadata(element));
            docs.add(javaChunk);
        }

        // 4. Use cases chunk
        if (element.getUseCases() != null && !element.getUseCases().isEmpty()) {
            EmbeddingDocument useCasesChunk = new EmbeddingDocument();
            useCasesChunk.setId(baseId + "-usecases");
            useCasesChunk.setDocumentId(baseId);
            useCasesChunk.setChunkType("use_cases");
            useCasesChunk.setText(String.format("%s is used for: %s",
                    element.getName(), String.join(", ", element.getUseCases())));
            useCasesChunk.setMetadata(createMetadata(element));
            docs.add(useCasesChunk);
        }

        return docs;
    }

    private Map<String, String> createMetadata(AndroidDocElement element) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("name", element.getName());
        metadata.put("type", element.getType().getDisplayName());
        metadata.put("category", element.getCategory().getDisplayName());
        metadata.put("package", element.getPackageName());
        if (element.getUrl() != null) {
            metadata.put("url", element.getUrl());
        }
        return metadata;
    }

    private String generateId(AndroidDocElement element) {
        return element.getFullName().replaceAll("[^a-zA-Z0-9.]", "_");
    }

    /**
     * LLM-optimized document structure
     */
    @lombok.Data
    public static class LLMDocument {
        private String id;
        private String name;
        private String fullName;
        private String type;
        private String category;
        private String packageName;
        private String content; // Combined searchable content
        private String summary;
        private String description;
        private String kotlinExample;
        private String javaExample;
        private List<String> useCases;
        private List<String> bestPractices;
        private List<String> relatedApis;
        private List<String> keyMethods;
        private String url;
    }

    /**
     * Embedding-ready document structure
     */
    @lombok.Data
    public static class EmbeddingDocument {
        private String id;
        private String documentId; // Reference to parent document
        private String chunkType; // overview, kotlin_example, java_example, etc.
        private String text; // The actual text to embed
        private Map<String, String> metadata;
    }
}
