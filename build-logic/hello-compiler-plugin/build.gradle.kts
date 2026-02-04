import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    kotlin("jvm")
}

group = "io.getstream.video.android.buildlogic"
version = "0.1.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.kotlin.compiler.embeddable)
}

tasks.withType<Jar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
