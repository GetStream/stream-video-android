import io.getstream.video.android.Configuration

plugins {
  kotlin("jvm")
}

rootProject.extra.apply {
  set("PUBLISH_GROUP_ID", Configuration.artifactGroup)
  set("PUBLISH_ARTIFACT_ID", "stream-video-android-bom")
  set("PUBLISH_VERSION", rootProject.extra.get("rootVersionName"))
}

dependencies {
  constraints {
    api(project(":stream-video-android-model"))
    api(project(":stream-video-android-core"))
    api(project(":stream-video-android-ui-common"))
    api(project(":stream-video-android-xml"))
    api(project(":stream-video-android-compose"))
    api(project(":stream-video-android-datastore"))
    api(project(":stream-video-android-tooling"))
    api(project(":stream-video-android-mock"))
  }
}

apply(from ="${rootDir}/scripts/publish-module.gradle")