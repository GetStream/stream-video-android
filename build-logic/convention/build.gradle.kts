plugins {
  `kotlin-dsl`
}

group = "io.getstream.video.android"

java {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
}

@Suppress("DSL_SCOPE_VIOLATION")
dependencies {
  compileOnly(libs.android.gradlePlugin)
  compileOnly(libs.kotlin.gradlePlugin)
  compileOnly(libs.compose.compiler.gradlePlugin)
//  compileOnly(libs.spotless.gradlePlugin)
}

gradlePlugin {
  plugins {
    register("androidApplicationCompose") {
      id = "io.getstream.android.application.compose"
      implementationClass = "AndroidApplicationComposeConventionPlugin"
    }
    register("androidApplication") {
      id = "io.getstream.android.application"
      implementationClass = "AndroidApplicationConventionPlugin"
    }
    register("androidLibraryCompose") {
      id = "io.getstream.android.library.compose"
      implementationClass = "AndroidLibraryComposeConventionPlugin"
    }
    register("androidLibrary") {
      id = "io.getstream.android.library"
      implementationClass = "AndroidLibraryConventionPlugin"
    }
//    register("spotless") {
//      id = "io.getstream.spotless"
//      implementationClass = "SpotlessConventionPlugin"
//    }
  }
}
