package io.getstream.video.android

object Configuration {
    const val compileSdk = 35
    const val targetSdk = 35
    const val minSdk = 24
    const val majorVersion = 1
    const val minorVersion = 4
    const val patchVersion = 7
    const val versionName = "$majorVersion.$minorVersion.$patchVersion"
    const val versionCode = 58
    const val snapshotVersionName = "$majorVersion.$minorVersion.${patchVersion + 1}-SNAPSHOT"
    const val artifactGroup = "io.getstream"
    const val streamVideoCallGooglePlayVersion = "1.4.7"
    const val streamWebRtcVersionName = "1.3.6"
}