plugins {
    alias(libs.plugins.dokka)
    alias(libs.plugins.stream.java.platform)
}

spotless {
    // Point to non-existent file pattern because this module has no source files
    java {
        target("no-files-to-format")
    }
    kotlin {
        target("no-files-to-format")
    }
}

dependencies {
    constraints {
        api(project(":stream-video-android-core"))
        api(project(":stream-video-android-ui-core"))
        api(project(":stream-video-android-ui-xml"))
        api(project(":stream-video-android-ui-compose"))
        api(project(":stream-video-android-filters-video"))
        api(project(":stream-video-android-previewdata"))
    }
}
