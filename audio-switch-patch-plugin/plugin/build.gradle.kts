import org.gradle.api.tasks.Sync

plugins {
    `java-gradle-plugin`
    `maven-publish`
    kotlin("jvm") version "1.9.25"
}

group = "io.getstream"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    compileOnly("com.android.tools.build:gradle-api:8.5.2")

    testImplementation(gradleApi())
    testImplementation(gradleTestKit())
    testImplementation("junit:junit:4.13.2")
}

val originalAudioSwitch by configurations.creating

dependencies {
    originalAudioSwitch("com.twilio:audioswitch:1.2.0@aar")
}

gradlePlugin {
    plugins {
        create("audioSwitchPatch") {
            id = "io.getstream.audioswitch-patch"
            implementationClass =
                "io.getstream.gradle.audioswitch.AudioSwitchPatchPlugin"
            displayName = "Stream AudioSwitch patch"
            description = "Replaces the Twilio AudioSwitch 1.2.0 class family with Stream's patched build"
        }
    }
}

val patchedAudioSwitchProject = project(":patched-audioswitch")
val compilePatchedAudioSwitch = patchedAudioSwitchProject.tasks.named("compileKotlin")

val generatePatchClasses by tasks.registering(Sync::class) {
    dependsOn(compilePatchedAudioSwitch)
    from(patchedAudioSwitchProject.layout.buildDirectory.dir("classes/kotlin/main")) {
        include("com/twilio/audioswitch/AudioSwitch.class")
        include("com/twilio/audioswitch/AudioSwitch\$*.class")
        into("patches/1.2.0/classes")
    }
    into(layout.buildDirectory.dir("generated/patch-resources"))

    doLast {
        val patchRoot = destinationDir.resolve("patches/1.2.0")
        val actual = patchRoot.resolve("classes")
            .walkTopDown()
            .filter(File::isFile)
            .map { it.relativeTo(patchRoot.resolve("classes")).invariantSeparatorsPath }
            .toSet()
        val expected = file("src/main/resources/patches/1.2.0/replacement-index.txt")
            .readLines()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .toSet()
        check(actual == expected) {
            "Replacement class inventory changed. Expected $expected, found $actual"
        }
    }
}

sourceSets.main {
    resources.srcDir(layout.buildDirectory.dir("generated/patch-resources"))
}

tasks.processResources {
    dependsOn(generatePatchClasses)
}

tasks.test {
    useJUnit()
    inputs.files(originalAudioSwitch)
    doFirst {
        systemProperty("audioswitch.original.aar", originalAudioSwitch.singleFile.absolutePath)
    }
}
