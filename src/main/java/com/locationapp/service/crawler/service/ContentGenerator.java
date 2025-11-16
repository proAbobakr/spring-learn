package com.locationapp.service.crawler.service;

import com.locationapp.service.crawler.model.AndroidDocElement;
import com.locationapp.service.crawler.model.AndroidDocType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Generates explanations, examples, and use cases for Android documentation
 */
@Slf4j
@Service
public class ContentGenerator {

    /**
     * Enhance documentation element with generated content
     */
    public void enhanceElement(AndroidDocElement element) {
        if (element == null) return;

        try {
            // Generate AI-friendly explanation
            element.setAiExplanation(generateExplanation(element));

            // Generate code examples if not present
            if (element.getExamples().isEmpty() || element.getKotlinExample() == null) {
                generateExamples(element);
            }

            // Generate use cases
            element.setUseCases(generateUseCases(element));

            // Generate best practices
            element.setBestPractices(generateBestPractices(element));

            // Generate related APIs
            element.setRelatedAPIs(generateRelatedAPIs(element));

            log.debug("Enhanced element: {}", element.getName());

        } catch (Exception e) {
            log.error("Error enhancing element: {}", element.getName(), e);
        }
    }

    /**
     * Generate a clear explanation of the API
     */
    private String generateExplanation(AndroidDocElement element) {
        StringBuilder explanation = new StringBuilder();

        explanation.append(String.format("## %s\n\n", element.getName()));

        // Type and category
        explanation.append(String.format("**Type:** %s  \n", element.getType().getDisplayName()));
        explanation.append(String.format("**Category:** %s  \n", element.getCategory().getDisplayName()));
        explanation.append(String.format("**Package:** `%s`  \n\n", element.getPackageName()));

        // Summary
        if (element.getSummary() != null && !element.getSummary().isEmpty()) {
            explanation.append("### Summary\n\n");
            explanation.append(element.getSummary()).append("\n\n");
        }

        // Description
        if (element.getDescription() != null && !element.getDescription().isEmpty()) {
            explanation.append("### Description\n\n");
            explanation.append(element.getDescription()).append("\n\n");
        }

        // Key points based on type
        explanation.append("### Key Points\n\n");
        explanation.append(generateKeyPoints(element));

        return explanation.toString();
    }

    private String generateKeyPoints(AndroidDocElement element) {
        List<String> points = new ArrayList<>();

        switch (element.getType()) {
            case CLASS:
                points.add("This is a class that can be instantiated or extended");
                if (element.getSuperClass() != null) {
                    points.add("Extends: `" + element.getSuperClass() + "`");
                }
                if (element.getMethods() != null && !element.getMethods().isEmpty()) {
                    points.add("Contains " + element.getMethods().size() + " methods");
                }
                break;

            case INTERFACE:
                points.add("This is an interface that defines a contract");
                points.add("Must be implemented by classes");
                if (element.getMethods() != null) {
                    points.add("Defines " + element.getMethods().size() + " methods");
                }
                break;

            case METHOD:
                points.add("This is a method that performs a specific operation");
                if (element.getReturnType() != null) {
                    points.add("Returns: `" + element.getReturnType() + "`");
                }
                break;

            case FIELD:
                points.add("This is a field/property that stores data");
                break;
        }

        return points.stream()
                .map(p -> "- " + p)
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");
    }

    /**
     * Generate code examples
     */
    private void generateExamples(AndroidDocElement element) {
        // Generate Kotlin example
        String kotlinExample = generateKotlinExample(element);
        element.setKotlinExample(kotlinExample);

        // Generate Java example
        String javaExample = generateJavaExample(element);
        element.setJavaExample(javaExample);

        // Add to examples list
        if (!kotlinExample.isEmpty()) {
            element.getExamples().add(AndroidDocElement.CodeExample.builder()
                    .title("Kotlin Example")
                    .language("kotlin")
                    .code(kotlinExample)
                    .explanation("Basic usage example in Kotlin")
                    .build());
        }

        if (!javaExample.isEmpty()) {
            element.getExamples().add(AndroidDocElement.CodeExample.builder()
                    .title("Java Example")
                    .language("java")
                    .code(javaExample)
                    .explanation("Basic usage example in Java")
                    .build());
        }
    }

    private String generateKotlinExample(AndroidDocElement element) {
        StringBuilder example = new StringBuilder();

        switch (element.getType()) {
            case CLASS:
                example.append("// Creating an instance\n");
                example.append("val instance = ").append(element.getName()).append("()\n\n");

                if (element.getMethods() != null && !element.getMethods().isEmpty()) {
                    example.append("// Using methods\n");
                    AndroidDocElement method = element.getMethods().get(0);
                    example.append("instance.").append(method.getName()).append("()\n");
                }
                break;

            case INTERFACE:
                example.append("// Implementing the interface\n");
                example.append("class MyImplementation : ").append(element.getName()).append(" {\n");
                if (element.getMethods() != null && !element.getMethods().isEmpty()) {
                    example.append("    override fun ").append(element.getMethods().get(0).getName())
                            .append("() {\n");
                    example.append("        // Implementation\n");
                    example.append("    }\n");
                }
                example.append("}\n");
                break;

            case METHOD:
                example.append("// Calling the method\n");
                example.append(element.getName()).append("()\n");
                break;
        }

        return example.toString();
    }

    private String generateJavaExample(AndroidDocElement element) {
        StringBuilder example = new StringBuilder();

        switch (element.getType()) {
            case CLASS:
                example.append("// Creating an instance\n");
                example.append(element.getName()).append(" instance = new ")
                        .append(element.getName()).append("();\n\n");

                if (element.getMethods() != null && !element.getMethods().isEmpty()) {
                    example.append("// Using methods\n");
                    AndroidDocElement method = element.getMethods().get(0);
                    example.append("instance.").append(method.getName()).append("();\n");
                }
                break;

            case INTERFACE:
                example.append("// Implementing the interface\n");
                example.append("public class MyImplementation implements ")
                        .append(element.getName()).append(" {\n");
                if (element.getMethods() != null && !element.getMethods().isEmpty()) {
                    example.append("    @Override\n");
                    example.append("    public void ").append(element.getMethods().get(0).getName())
                            .append("() {\n");
                    example.append("        // Implementation\n");
                    example.append("    }\n");
                }
                example.append("}\n");
                break;

            case METHOD:
                example.append("// Calling the method\n");
                example.append(element.getName()).append("();\n");
                break;
        }

        return example.toString();
    }

    /**
     * Generate use cases
     */
    private List<String> generateUseCases(AndroidDocElement element) {
        List<String> useCases = new ArrayList<>();

        switch (element.getCategory()) {
            case UI_COMPONENTS:
                useCases.add("Building user interfaces");
                useCases.add("Creating custom views");
                useCases.add("Handling user interactions");
                break;

            case ACTIVITIES_FRAGMENTS:
                useCases.add("Managing app screens and navigation");
                useCases.add("Handling lifecycle events");
                useCases.add("Organizing UI components");
                break;

            case DATA_STORAGE:
                useCases.add("Persisting app data");
                useCases.add("Managing databases");
                useCases.add("Storing user preferences");
                break;

            case NETWORKING:
                useCases.add("Making HTTP requests");
                useCases.add("Communicating with APIs");
                useCases.add("Downloading/uploading data");
                break;

            case BACKGROUND_TASKS:
                useCases.add("Running long operations");
                useCases.add("Scheduling periodic tasks");
                useCases.add("Processing data in background");
                break;

            default:
                useCases.add("General Android development");
                useCases.add("Building mobile applications");
        }

        return useCases;
    }

    /**
     * Generate best practices
     */
    private List<String> generateBestPractices(AndroidDocElement element) {
        List<String> practices = new ArrayList<>();

        practices.add("Follow Android's official documentation");
        practices.add("Test thoroughly on different Android versions");

        switch (element.getCategory()) {
            case UI_COMPONENTS:
                practices.add("Use ConstraintLayout for complex layouts");
                practices.add("Implement proper accessibility features");
                practices.add("Handle different screen sizes");
                break;

            case ACTIVITIES_FRAGMENTS:
                practices.add("Always handle lifecycle callbacks properly");
                practices.add("Avoid memory leaks by cleaning up resources");
                practices.add("Use ViewModel for data persistence");
                break;

            case DATA_STORAGE:
                practices.add("Use Room for database operations");
                practices.add("Implement proper data migration strategies");
                practices.add("Never perform database operations on main thread");
                break;

            case NETWORKING:
                practices.add("Always use background threads for network calls");
                practices.add("Implement proper error handling");
                practices.add("Cache data when appropriate");
                practices.add("Handle network connectivity changes");
                break;

            case BACKGROUND_TASKS:
                practices.add("Use WorkManager for guaranteed execution");
                practices.add("Minimize battery consumption");
                practices.add("Respect system constraints");
                break;
        }

        return practices;
    }

    /**
     * Generate related APIs
     */
    private List<String> generateRelatedAPIs(AndroidDocElement element) {
        List<String> related = new ArrayList<>();

        // Add parent class as related
        if (element.getSuperClass() != null) {
            related.add(element.getSuperClass());
        }

        // Add interfaces as related
        if (element.getInterfaces() != null) {
            related.addAll(element.getInterfaces());
        }

        // Add category-specific related APIs
        switch (element.getCategory()) {
            case UI_COMPONENTS:
                related.addAll(Arrays.asList("View", "ViewGroup", "LayoutInflater"));
                break;

            case ACTIVITIES_FRAGMENTS:
                related.addAll(Arrays.asList("Activity", "Fragment", "ViewModel", "LiveData"));
                break;

            case DATA_STORAGE:
                related.addAll(Arrays.asList("Room", "SharedPreferences", "SQLiteDatabase"));
                break;

            case NETWORKING:
                related.addAll(Arrays.asList("OkHttpClient", "Retrofit", "HttpURLConnection"));
                break;
        }

        return related.stream().distinct().limit(10).toList();
    }
}
