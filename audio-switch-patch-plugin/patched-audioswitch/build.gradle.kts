import org.gradle.api.tasks.Sync
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    kotlin("jvm") version "1.9.25"
}

val audioSwitchVersion = "1.2.0"
val upstreamSources by configurations.creating

dependencies {
    upstreamSources("com.twilio:audioswitch:$audioSwitchVersion:sources@jar")
    compileOnly(files(androidJar()))
    compileOnly("androidx.annotation:annotation:1.8.2")
}

val extractUpstreamSources by tasks.registering(Sync::class) {
    from({ upstreamSources.map(::zipTree) }) {
        exclude("META-INF/**")
        exclude("com/twilio/audioswitch/AudioSwitch.kt")
        exclude("com/twilio/audioswitch/BuildConfig.java")
    }
    into(layout.buildDirectory.dir("generated/upstream-sources"))
}

sourceSets.main {
    kotlin.srcDir(extractUpstreamSources.map { it.destinationDir })
}

tasks.named("compileKotlin").configure {
    dependsOn(extractUpstreamSources)
}

kotlin {
    jvmToolchain(17)

    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
        moduleName.set("audioswitch_release")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

fun Project.androidJar(): File {
    val sdkPath = System.getenv("ANDROID_HOME")
        ?: System.getenv("ANDROID_SDK_ROOT")
        ?: Properties().run {
            val localProperties = rootDir.resolve("../local.properties")
            if (localProperties.isFile) {
                localProperties.inputStream().use(::load)
            }
            getProperty("sdk.dir")
        }
        ?: error("Android SDK not found. Set ANDROID_HOME or sdk.dir in the repository local.properties")

    return file(sdkPath).resolve("platforms/android-35/android.jar").also { jar ->
        check(jar.isFile) { "Android 35 platform not found at $jar" }
    }
}
