import io.getstream.video.android.Configuration
import io.getstream.video.android.Dependencies

plugins {
    id("io.getstream.android.library.compose")
    id("io.getstream.spotless")
}

rootProject.extra.apply {
    set("PUBLISH_GROUP_ID", Configuration.artifactGroup)
    set("PUBLISH_ARTIFACT_ID", "stream-video-android-compose")
    set("PUBLISH_VERSION", rootProject.extra.get("rootVersionName"))
}

apply(from = "${rootDir}/scripts/publish-module.gradle")

android {
    compileSdk = Configuration.compileSdk

    defaultConfig {
        minSdk = Configuration.minSdk
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().all {
    kotlinOptions.freeCompilerArgs += listOf(
        "-P",
        "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=" +
                project.buildDir.absolutePath + "/compose_metrics"
    )
    kotlinOptions.freeCompilerArgs += listOf(
        "-P",
        "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=" +
                project.buildDir.absolutePath + "/compose_metrics"
    )
}

dependencies {
    api(project(":stream-video-android-core"))
    api(project(":stream-video-android-ui-common"))

    implementation(Dependencies.material)
    implementation(Dependencies.streamLog)

    // Compose
    implementation(platform(Dependencies.composeBom))
    implementation(Dependencies.composeRuntime)
    implementation(Dependencies.composeUi)
    implementation(Dependencies.composeUiTooling)
    implementation(Dependencies.composeFoundation)
    implementation(Dependencies.composeMaterial)
    implementation(Dependencies.activityCompose)
    implementation(Dependencies.composeIconsExtended)
    implementation(Dependencies.composeCoil)
}