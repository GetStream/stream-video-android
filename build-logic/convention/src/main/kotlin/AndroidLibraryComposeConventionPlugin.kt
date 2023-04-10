import com.android.build.gradle.LibraryExtension
import io.getstream.video.configureAndroidCompose
import io.getstream.video.configureKotlinAndroid
import io.getstream.video.kotlinOptions
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLibraryComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")
            pluginManager.apply("org.jetbrains.kotlin.android")
            pluginManager.apply("binary-compatibility-validator")
            pluginManager.apply("org.jetbrains.dokka")
            pluginManager.apply("app.cash.paparazzi")

            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)
                configureAndroidCompose(this)

                packagingOptions {
                    resources {
                        excludes += "/META-INF/{AL2.0,LGPL2.1}"
                    }
                }

                kotlinOptions {
                    freeCompilerArgs = freeCompilerArgs + listOf("-Xexplicit-api=strict")
                }
            }
        }
    }
}
