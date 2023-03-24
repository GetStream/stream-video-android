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

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    api(project(":stream-video-android-core"))

    implementation(Dependencies.material)
    implementation(Dependencies.streamLog)
    implementation(Dependencies.coil)

    implementation(Dependencies.androidxActivity)
    implementation(Dependencies.androidxLifecycleRuntime)
}