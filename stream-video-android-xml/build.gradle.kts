import io.getstream.video.android.Configuration
import io.getstream.video.android.Dependencies

plugins {
    id("io.getstream.android.library")
    id("io.getstream.spotless")
}

rootProject.extra.apply {
    set("PUBLISH_GROUP_ID", Configuration.artifactGroup)
    set("PUBLISH_ARTIFACT_ID", "stream-video-android-xml")
    set("PUBLISH_VERSION", rootProject.extra.get("rootVersionName"))
}

apply(from ="${rootDir}/scripts/publish-module.gradle")

android {
    compileSdk = Configuration.compileSdk

    defaultConfig {
        minSdk = Configuration.minSdk
    }

    resourcePrefix = "stream_video_"

    buildFeatures {
        viewBinding = true
    }

    lint {
        baseline = file("lint-baseline.xml")
    }
}

dependencies {
    api(project(":stream-video-android-core"))
    api(project(":stream-video-android-ui-common"))

    implementation(Dependencies.material)
    implementation(Dependencies.streamLog)
    implementation(Dependencies.coil)

    // AndroidX
    implementation(Dependencies.androidxActivity)
    implementation(Dependencies.androidxLifecycleRuntime)
    implementation(Dependencies.androidxStartup)
}