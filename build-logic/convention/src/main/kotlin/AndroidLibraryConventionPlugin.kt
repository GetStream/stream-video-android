import com.android.build.gradle.LibraryExtension
import io.getstream.video.configureKotlinAndroid
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")
            pluginManager.apply("org.jetbrains.kotlin.android")
            pluginManager.apply("binary-compatibility-validator")
            pluginManager.apply("org.jetbrains.dokka")

            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)

//                kotlinOptions {
//                    freeCompilerArgs = freeCompilerArgs + listOf("-Xexplicit-api=strict")
//                }
            }
        }
    }
}
