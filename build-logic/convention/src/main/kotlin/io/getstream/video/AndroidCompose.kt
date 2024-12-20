package io.getstream.video

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
// import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension
// import org.jetbrains.kotlin.compose.compiler.gradle.ComposeFeatureFlag

/**
 * Configure Compose-specific options
 */
internal fun Project.configureAndroidCompose(
  commonExtension: CommonExtension<*, *, *, *, *, *>,
) {
//  pluginManager.apply("org.jetbrains.kotlin.plugin.compose") -> Enable with Kotlin 2.0+
  val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

  commonExtension.apply {
    buildFeatures {
      compose = true
    }

      // Remove this with Kotlin 2.0+
      composeOptions {
          kotlinCompilerExtensionVersion = "1.5.15"
      }
  }

  dependencies {
    val bom = libs.findLibrary("androidx-compose-bom").get()
    add("implementation", platform(bom))
    add("androidTestImplementation", platform(bom))
  }

//  extensions.configure<ComposeCompilerGradlePluginExtension> { -> Enable with Kotlin 2.0+
//      featureFlags.addAll(ComposeFeatureFlag.StrongSkipping, ComposeFeatureFlag.IntrinsicRemember)
//    reportsDestination = layout.buildDirectory.dir("compose_compiler")
//    stabilityConfigurationFile = rootProject.layout.projectDirectory.file("compose_compiler_config.conf")
//  }
}