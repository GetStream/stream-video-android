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

plugins {
  alias(libs.plugins.stream.project)
  alias(libs.plugins.stream.android.application) apply false
  alias(libs.plugins.stream.android.library) apply false
  alias(libs.plugins.stream.android.test) apply false
  alias(libs.plugins.stream.java.library) apply false
  alias(libs.plugins.stream.java.platform) apply false
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.android) apply false
  // alias(libs.plugins.compose.compiler) apply false -> Enable with Kotlin 2.0+
  alias(libs.plugins.kotlin.serialization) apply false
  alias(libs.plugins.kotlin.compatibility.validator) apply false
  alias(libs.plugins.ksp) apply false
  alias(libs.plugins.wire) apply false
  alias(libs.plugins.google.gms) apply false
  alias(libs.plugins.dokka)
  alias(libs.plugins.spotless) apply false
  alias(libs.plugins.paparazzi) apply false
  alias(libs.plugins.firebase.crashlytics) apply false
  alias(libs.plugins.hilt) apply false
  alias(libs.plugins.play.publisher) apply false
  alias(libs.plugins.baseline.profile) apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.6"

}

streamProject {
    spotless {
        excludePatterns = setOf("**/generated/**")
    }

    coverage {
        includedModules = setOf(
            "stream-video-android-core",
            "stream-video-android-ui-compose",
        )
        sonarCoverageExclusions = listOf(
            "**/*.mp3",
            "**/*.webp",
            "**/generated/**",
            "**/io/getstream/video/android/core/model/**"
        )
        koverClassExclusions = listOf(
            "io.getstream.android.video.generated.*",
            "io.getstream.video.android.core.model.*"
        )
    }

    publishing {
        description = "Stream Video official Android SDK"
        moduleArtifactIdOverrides = mapOf(
            "stream-video-android-ui-xml" to "stream-video-android-xml"
        )
    }
}

// ─── Detekt ──────────────────────────────────────────────────────────────────
val buildSrcJar = fileTree("${rootProject.rootDir}/buildSrc/build/libs") {
    include("buildSrc*.jar")
}

subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")

    configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        toolVersion = "1.23.6"
        config.setFrom(rootProject.files("config/detekt/detekt.yml"))
        buildUponDefaultConfig = true
        // Baseline snapshots pre-existing violations so only new issues break the build.
        // Generate/regenerate with: ./gradlew detektBaseline
        baseline = rootProject.file("config/detekt/baseline-${project.name}.xml")
    }

    // Custom rules live in buildSrc; add its compiled jar as a detekt plugin so
    // the service-loader picks up StreamRuleSetProvider during analysis.
    dependencies {
        "detektPlugins"(buildSrcJar)
    }

    tasks.register<io.gitlab.arturbosch.detekt.Detekt>("detektStreamRules") {
        description = "Runs only Stream custom detekt rules (silent — use detektSummary for output)"
        setSource(files("src"))
        config.setFrom(rootProject.files("config/detekt/detekt.yml"))
        buildUponDefaultConfig = false
        disableDefaultRuleSets = true
        ignoreFailures = true
        include("**/*.kt")
        reports {
            xml.required.set(true)
            xml.outputLocation.set(layout.buildDirectory.file("reports/detekt/streamRules.xml"))
            html.required.set(false)
            sarif.required.set(false)
            md.required.set(false)
            txt.required.set(false)
        }
        classpath.setFrom()
        pluginClasspath.setFrom(buildSrcJar)
        // Demote detekt's own stdout (progress dots, stats) so only detektSummary prints.
        logging.captureStandardOutput(LogLevel.DEBUG)
    }

    tasks.register("detektSummary") {
        description = "Runs Stream custom detekt rules and prints a concise violation summary"
        dependsOn("detektStreamRules")
        doLast {
            val reportFile = layout.buildDirectory.file("reports/detekt/streamRules.xml").get().asFile
            if (!reportFile.exists()) {
                logger.quiet("detektSummary: no report found at ${reportFile.path}")
                return@doLast
            }

            val doc = javax.xml.parsers.DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(reportFile)

            val projectPath = rootProject.projectDir.absolutePath + "/"
            val fileNodes = doc.getElementsByTagName("file")
            val violations = mutableListOf<String>()

            for (i in 0 until fileNodes.length) {
                val fileNode = fileNodes.item(i) as org.w3c.dom.Element
                val fullPath = fileNode.getAttribute("name")
                val relPath = fullPath.removePrefix(projectPath)
                val errors = fileNode.getElementsByTagName("error")
                for (j in 0 until errors.length) {
                    val err = errors.item(j) as org.w3c.dom.Element
                    violations += "  $relPath:${err.getAttribute("line")} → ${err.getAttribute("message")}"
                }
            }

            if (violations.isEmpty()) {
                logger.quiet("\n✓ No Stream rule violations found in ${project.name}.")
            } else {
                logger.quiet("\n=== Stream Detekt: ${project.name} (${violations.size} violation(s)) ===")
                violations.forEach { logger.quiet(it) }
                logger.quiet("")
                throw GradleException("detektSummary: ${violations.size} violation(s) found in ${project.name}.")
            }
        }
    }
}

// Runs StreamRules only on files that are new (untracked) or staged-as-added in git.
// Usage: ./gradlew detektNewFiles
tasks.register<io.gitlab.arturbosch.detekt.Detekt>("detektNewFiles") {
    description = "Runs Stream custom detekt rules on new/untracked git files only"

    val out = java.io.ByteArrayOutputStream()
    // staged new files
    exec { commandLine("git", "diff", "--name-only", "--diff-filter=A", "HEAD"); standardOutput = out; isIgnoreExitValue = true }
    // untracked files not yet staged
    exec { commandLine("git", "ls-files", "--others", "--exclude-standard"); standardOutput = out; isIgnoreExitValue = true }

    val newKtFiles = out.toString().trim().lines()
        .filter { it.endsWith(".kt") }
        .map { rootProject.file(it) }
        .filter { it.exists() }

    setSource(files(newKtFiles))
    config.setFrom(rootProject.files("config/detekt/detekt.yml"))
    buildUponDefaultConfig = false
    disableDefaultRuleSets = true
    include("**/*.kt")
    reports { html.required.set(false); xml.required.set(false) }
    classpath.setFrom()
    pluginClasspath.setFrom(buildSrcJar)
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
