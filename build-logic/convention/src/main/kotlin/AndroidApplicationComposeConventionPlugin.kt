import com.android.build.api.dsl.ApplicationExtension
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import io.getstream.video.configureAndroidCompose
import io.getstream.video.configureFlavors
import io.getstream.video.configureKotlinAndroid
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.dependencies

class AndroidApplicationComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")
            pluginManager.apply("org.jetbrains.kotlin.android")

            extensions.configure<BaseAppModuleExtension> {
                configureKotlinAndroid(this)
                configureAndroidCompose(this)

                packaging {
                    resources {
                        excludes += "/META-INF/{AL2.0,LGPL2.1}"
                    }
                }
            }

            extensions.configure<ApplicationExtension> {
                configureFlavors(this)
            }

            val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
            dependencies {
                "implementation"(libs.findLibrary("androidx.compose.tracing").get())
            }
        }
    }
}
