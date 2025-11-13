plugins {
    alias(libs.plugins.maven.publish)
}

val aarFile = file("libs/libwebrtc_repackaged.aar")

configurations.maybeCreate("default")
artifacts.add("default", aarFile)

val androidSourcesJar = tasks.register<Jar>("androidSourcesJar") {
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
    archiveBaseName.set("stream-video-android-repackaged-webrtc")
    archiveClassifier.set("sources")
    // This project only contains pre-built AAR artifacts, no source code
    // Create an empty sources jar to satisfy Maven Central requirements
    from(file("libs/"))
    includeEmptyDirs = false
}

val javadocJar = tasks.register<Jar>("javadocJar") {
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
    archiveBaseName.set("stream-video-android-repackaged-webrtc")
    archiveClassifier.set("javadoc")
}

mavenPublishing {
    coordinates(
        groupId = "io.getstream",
        artifactId = "stream-video-android-repackaged-webrtc",
        version = rootProject.version.toString()
    )

}

// Manually create the publication to include the pre-built AAR because that's not configurable
// through the maven.publish plugin.
afterEvaluate {
    publishing {
        publications.create<MavenPublication>("prebuitltAar") {
            // Add pre-built AAR and the sources/javadoc jars
            artifact(aarFile)
            artifact(androidSourcesJar)
            artifact(javadocJar)
        }
    }
}
