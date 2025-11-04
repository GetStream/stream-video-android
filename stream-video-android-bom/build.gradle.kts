import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import io.getstream.video.android.Configuration

plugins {
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.dokka)
    kotlin("jvm")
}

dependencies {
  constraints {
    api(project(":stream-video-android-core"))
    api(project(":stream-video-android-ui-core"))
    api(project(":stream-video-android-ui-xml"))
    api(project(":stream-video-android-ui-compose"))
    api(project(":stream-video-android-filters-video"))
    api(project(":stream-video-android-previewdata"))
  }
}

mavenPublishing {
    coordinates(
        groupId = Configuration.artifactGroup,
        artifactId = "stream-video-android-bom",
        version = rootProject.version.toString(),
    )
    configure(
        KotlinJvm(
            javadocJar = JavadocJar.Dokka("dokkaJavadoc"),
            sourcesJar = true,
        ),
    )
}
