package com.locationapp.service.crawler.model;

/**
 * Enum representing different types of Android documentation entities
 */
public enum AndroidDocType {
    CLASS("Class"),
    INTERFACE("Interface"),
    ENUM("Enum"),
    ANNOTATION("Annotation"),
    PACKAGE("Package"),
    METHOD("Method"),
    FIELD("Field"),
    CONSTRUCTOR("Constructor"),
    CONSTANT("Constant");

    private final String displayName;

    AndroidDocType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
