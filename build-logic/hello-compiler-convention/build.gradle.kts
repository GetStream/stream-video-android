import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    `kotlin-dsl`
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
    implementation(libs.kotlin.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("helloCompilerConvention") {
            id = "io.getstream.video.buildlogic.hello-compiler-convention"
            implementationClass = "io.getstream.video.buildlogic.convention.HelloCompilerConventionPlugin"
        }
    }
}
