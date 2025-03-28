apply(plugin = "io.github.gradle-nexus.publish-plugin")
apply(plugin = "org.jetbrains.dokka")
apply(from = "${rootDir}/scripts/sonar.gradle")
apply(from = "${rootDir}/scripts/open-api-code-gen.gradle.kts")

buildscript {
  repositories {
    google()
    mavenCentral()
    maven("https://plugins.gradle.org/m2/")
  }

  dependencies {
    // TODO: Remove this workaround after AGP 8.9.0 is released
    // Workaround for integrate sonarqube plugin with AGP
    // It looks like will be fixed after AGP 8.9.0-alpha04 is released
    // https://issuetracker.google.com/issues/380600747?pli=1
    classpath("org.bouncycastle:bcutil-jdk18on:1.79")
  }
}

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.android) apply false
  // alias(libs.plugins.compose.compiler) apply false -> Enable with Kotlin 2.0+
  alias(libs.plugins.kotlin.serialization) apply false
  alias(libs.plugins.kotlin.compatibility.validator) apply false
  alias(libs.plugins.ksp) apply false
  alias(libs.plugins.wire) apply false
  alias(libs.plugins.nexus) apply false
  alias(libs.plugins.google.gms) apply false
  alias(libs.plugins.dokka) apply false
  alias(libs.plugins.spotless) apply false
  alias(libs.plugins.paparazzi) apply false
  alias(libs.plugins.firebase.crashlytics) apply false
  alias(libs.plugins.hilt) apply false
  alias(libs.plugins.play.publisher) apply false
  alias(libs.plugins.baseline.profile) apply false
  alias(libs.plugins.sonarqube) apply false
  alias(libs.plugins.kover) apply false
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
  if (name.startsWith("stream-video-android")
      && !name.startsWith("stream-video-android-core")
      && !name.contains("metrics")) {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
      kotlinOptions.freeCompilerArgs += listOf(
        "-Xexplicit-api=strict"
      )
    }
  }

  apply(from = "${rootDir}/scripts/coverage.gradle")
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

afterEvaluate {
    println("Running Add Pre Commit Git Hook Script on Build")
    exec {
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            // Windows-specific command
            commandLine("cmd", "/c", "copy", ".\\scripts\\git-hooks\\pre-push", ".\\.git\\hooks")
        } else {
            // Unix-based systems
            commandLine("cp", "./scripts/git-hooks/pre-push", "./.git/hooks")
        }
    }
    println("Added pre-push Git Hook Script.")
}