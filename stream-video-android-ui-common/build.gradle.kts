import io.getstream.video.android.Configuration

@Suppress("DSL_SCOPE_VIOLATION")
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

    implementation(libs.androidx.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.lifecycle.runtime)

    implementation(libs.coil)

    implementation(libs.stream.log)
}