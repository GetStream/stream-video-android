import com.android.build.gradle.LibraryExtension
import io.getstream.video.configureKotlinAndroid
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")
            pluginManager.apply("org.jetbrains.kotlin.android")
            pluginManager.apply("binary-compatibility-validator")
            pluginManager.apply("org.jetbrains.dokka")

            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)

                val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

                tasks.withType(JavaCompile::class.java).configureEach {
                    this.targetCompatibility = libs.findVersion("jvmTarget").get().toString()
                    this.sourceCompatibility = libs.findVersion("jvmTarget").get().toString()
                }

//                kotlinOptions {
//                    freeCompilerArgs = freeCompilerArgs + listOf("-Xexplicit-api=strict")
//                }
            }
        }
    }
}
