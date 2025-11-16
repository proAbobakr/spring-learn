package com.locationapp.service.crawler.model;

/**
 * Major categories for Android APIs
 */
public enum AndroidCategory {
    UI_COMPONENTS("UI & Views", "android.widget, android.view"),
    ACTIVITIES_FRAGMENTS("Activities & Fragments", "android.app, androidx.fragment"),
    LIFECYCLE("Lifecycle & Architecture", "androidx.lifecycle"),
    NAVIGATION("Navigation", "androidx.navigation"),
    DATA_STORAGE("Data & Storage", "android.database, androidx.room, android.content"),
    NETWORKING("Networking", "android.net, okhttp3, retrofit2"),
    MEDIA("Media & Graphics", "android.media, android.graphics"),
    SENSORS_LOCATION("Sensors & Location", "android.location, android.hardware"),
    PERMISSIONS_SECURITY("Permissions & Security", "android.permission, androidx.security"),
    BACKGROUND_TASKS("Background Work", "androidx.work, android.app.job"),
    NOTIFICATIONS("Notifications", "android.app.Notification, androidx.core.app.NotificationCompat"),
    MATERIAL_DESIGN("Material Design", "com.google.android.material"),
    JETPACK_COMPOSE("Jetpack Compose", "androidx.compose"),
    TESTING("Testing", "androidx.test, junit"),
    DEPENDENCY_INJECTION("Dependency Injection", "dagger, hilt"),
    COROUTINES("Kotlin Coroutines", "kotlinx.coroutines"),
    VIEWMODEL_LIVEDATA("ViewModel & LiveData", "androidx.lifecycle.ViewModel, androidx.lifecycle.LiveData"),
    RECYCLER_VIEW("RecyclerView", "androidx.recyclerview"),
    INTENT_SERVICES("Intents & Services", "android.content.Intent, android.app.Service"),
    RESOURCES("Resources & Assets", "android.content.res"),
    UTILITIES("Utilities & Helpers", "android.util, android.text"),
    CORE("Core Android", "android.os, android.content"),
    OTHER("Other", "");

    private final String displayName;
    private final String packagePrefixes;

    AndroidCategory(String displayName, String packagePrefixes) {
        this.displayName = displayName;
        this.packagePrefixes = packagePrefixes;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPackagePrefixes() {
        return packagePrefixes;
    }

    public static AndroidCategory categorizeByPackage(String packageName) {
        if (packageName == null) return OTHER;

        for (AndroidCategory category : values()) {
            if (category == OTHER) continue;

            String[] prefixes = category.packagePrefixes.split(",\\s*");
            for (String prefix : prefixes) {
                if (packageName.startsWith(prefix)) {
                    return category;
                }
            }
        }
        return OTHER;
    }
}
