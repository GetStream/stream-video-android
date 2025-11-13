import com.vanniktech.maven.publish.MavenPublishBaseExtension
import io.getstream.video.android.Configuration
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

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
  alias(libs.plugins.stream.android.application) apply false
  alias(libs.plugins.stream.android.library) apply false
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.android) apply false
  // alias(libs.plugins.compose.compiler) apply false -> Enable with Kotlin 2.0+
  alias(libs.plugins.kotlin.serialization) apply false
  alias(libs.plugins.kotlin.compatibility.validator) apply false
  alias(libs.plugins.ksp) apply false
  alias(libs.plugins.wire) apply false
  alias(libs.plugins.maven.publish)
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
    alias(libs.plugins.android.library) apply false
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
}

private val isSnapshot = System.getenv("SNAPSHOT")?.toBoolean() == true

version = if (isSnapshot) {
    val timestamp = SimpleDateFormat("yyyyMMddHHmm").run {
        timeZone = TimeZone.getTimeZone("UTC")
        format(Date())
    }
    "${Configuration.snapshotBasedVersionName}-${timestamp}-SNAPSHOT"
} else {
    Configuration.versionName
}

subprojects {
  plugins.withId("com.vanniktech.maven.publish") {
    extensions.configure<MavenPublishBaseExtension> {
      publishToMavenCentral(automaticRelease = true)

      pom {
        name.set(project.name)
        description.set("Stream Video official Android SDK")
        url.set("https://github.com/getstream/stream-video-android")

        licenses {
          license {
            name.set("Stream License")
            url.set("https://github.com/GetStream/stream-video-android/blob/main/LICENSE")
          }
        }

        developers {
          developer {
            id = "aleksandar-apostolov"
            name = "Aleksandar Apostolov"
            email = "aleksandar.apostolov@getstream.io"
          }
          developer {
            id = "VelikovPetar"
            name = "Petar Velikov"
            email = "petar.velikov@getstream.io"
          }
          developer {
            id = "andremion"
            name = "André Mion"
            email = "andre.rego@getstream.io"
          }
          developer {
            id = "rahul-lohra"
            name = "Rahul Kumar Lohra"
            email = "rahul.lohra@getstream.io"
          }
          developer {
            id = "PratimMallick"
            name = "Pratim Mallick"
            email = "pratim.mallick@getstream.io"
          }
          developer {
            id = "gpunto"
            name = "Gianmarco David"
            email = "gianmarco.david@getstream.io"
          }
        }

        scm {
          connection.set("scm:git:github.com/getstream/stream-video-android.git")
          developerConnection.set("scm:git:ssh://github.com/getstream/stream-video-android.git")
          url.set("https://github.com/getstream/stream-video-android/tree/main")
        }
      }
    }
  }
}

tasks.register("printAllArtifacts") {
  group = "publishing"
  description = "Prints all artifacts that will be published"

  doLast {
    subprojects.forEach { subproject ->
      subproject.plugins.withId("com.vanniktech.maven.publish") {
        subproject.extensions.findByType(PublishingExtension::class.java)
          ?.publications
          ?.filterIsInstance<MavenPublication>()
          ?.forEach { println("${it.groupId}:${it.artifactId}:${it.version}") }
      }
    }
  }
}

//apply(from = teamPropsFile("git-hooks.gradle.kts"))
//
//fun teamPropsFile(propsFile: String): File {
//    val teamPropsDir = file("team-props")
//    return File(teamPropsDir, propsFile)
//}

apply(from = "${rootDir}/scripts/sonar.gradle")
apply(from = "${rootDir}/scripts/coverage.gradle")

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
