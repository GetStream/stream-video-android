plugins {
    alias(libs.plugins.dokka)
    kotlin("jvm")
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
