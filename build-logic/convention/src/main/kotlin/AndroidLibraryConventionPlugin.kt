import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import io.getstream.video.configureKotlinAndroid
import io.getstream.video.kotlinOptions
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")
            pluginManager.apply("org.jetbrains.kotlin.android")
            pluginManager.apply("binary-compatibility-validator")
            pluginManager.apply("org.jetbrains.dokka")

            extensions.configure<BaseAppModuleExtension> {
                configureKotlinAndroid(this)

//                kotlinOptions {
//                    freeCompilerArgs = freeCompilerArgs + listOf("-Xexplicit-api=strict")
//                }
            }
        }
    }
}
