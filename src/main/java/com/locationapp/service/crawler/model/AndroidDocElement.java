package com.locationapp.service.crawler.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single Android documentation element (class, method, etc.)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AndroidDocElement {

    private String name;
    private String fullName;
    private String packageName;
    private AndroidDocType type;
    private AndroidCategory category;
    private String url;

    // Documentation content
    private String summary;
    private String description;
    private String syntax;

    // Code examples
    @Builder.Default
    private List<CodeExample> examples = new ArrayList<>();

    // For methods/constructors
    private List<Parameter> parameters;
    private String returnType;
    private String returnDescription;
    private List<String> throwsExceptions;

    // Relationships
    private String superClass;
    private List<String> interfaces;
    private List<String> subClasses;
    private List<AndroidDocElement> methods;
    private List<AndroidDocElement> fields;
    private List<AndroidDocElement> constants;

    // Metadata
    private String deprecated;
    private String sinceApiLevel;
    private List<String> tags;

    // Generated content
    private String aiExplanation;
    private String kotlinExample;
    private String javaExample;
    private List<String> useCases;
    private List<String> bestPractices;
    private List<String> relatedAPIs;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Parameter {
        private String name;
        private String type;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CodeExample {
        private String title;
        private String language; // kotlin, java
        private String code;
        private String explanation;
    }
}
