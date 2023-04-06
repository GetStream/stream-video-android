apply(plugin = "io.github.gradle-nexus.publish-plugin")
apply(plugin = "org.jetbrains.dokka")

buildscript {
    repositories {
        google()
        mavenCentral()
        maven("https://plugins.gradle.org/m2/")
    }
}

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.compatibility.validator) apply false
    alias(libs.plugins.wire) apply false
    alias(libs.plugins.nexus) apply false
    alias(libs.plugins.google.gms) apply false
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.spotless) apply false
}

subprojects {
    if (name.startsWith("stream-video-android")) {
        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
            kotlinOptions.freeCompilerArgs += listOf(
                "-Xjvm-default=enable",
                "-opt-in=io.getstream.video.android.core.internal.InternalStreamVideoApi"
            )
        }
    }

    // TODO - re-enable the core module once coordinator is stable
    if (name.startsWith("stream-video-android") && !name.startsWith("stream-video-android-core")) {
        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
            kotlinOptions.freeCompilerArgs += listOf(
                "-Xexplicit-api=strict"
            )
        }
    }
}

tasks.register("clean")
    .configure {
        delete(rootProject.buildDir)
    }

apply(from = "${rootDir}/scripts/publish-root.gradle")
//apply(from = teamPropsFile("git-hooks.gradle.kts"))
//
//fun teamPropsFile(propsFile: String): File {
//    val teamPropsDir = file("team-props")
//    return File(teamPropsDir, propsFile)
//}