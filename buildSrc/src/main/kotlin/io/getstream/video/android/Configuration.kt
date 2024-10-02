package io.getstream.video.android

object Configuration {
    const val compileSdk = 34
    const val targetSdk = 34
    const val minSdk = 24
    const val majorVersion = 1
    const val minorVersion = 0
    const val patchVersion = 15
    const val versionName = "$majorVersion.$minorVersion.$patchVersion"
    const val versionCode = 39
    const val snapshotVersionName = "$majorVersion.$minorVersion.${patchVersion + 1}-SNAPSHOT"
    const val artifactGroup = "io.getstream"
    const val streamVideoCallGooglePlayVersion = "1.1.8"
    const val streamWebRtcVersionName = "1.1.1"
}
