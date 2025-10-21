import com.android.build.gradle.LibraryExtension
import io.getstream.video.configureKotlinAndroid
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("io.getstream.android.library")
            pluginManager.apply("org.jetbrains.kotlin.android")
            pluginManager.apply("binary-compatibility-validator")
            pluginManager.apply("org.jetbrains.dokka")
            pluginManager.apply("androidx.baselineprofile")

            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)

//                kotlinOptions {
//                    freeCompilerArgs = freeCompilerArgs + listOf("-Xexplicit-api=strict")
//                }

                dependencies {
                    add("baselineProfile", project(":benchmark"))
                }
            }
        }
    }
}
