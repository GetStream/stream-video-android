package io.getstream.video.buildlogic.convention

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

private const val HELLO_COMPILER_PLUGIN_ID = ":hello-compiler-plugin"
private const val COMPILER_PLUGIN_CONFIGURATION = "kotlinCompilerPluginClasspath"

class HelloCompilerConventionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val pluginProject = project.rootProject.findProject(HELLO_COMPILER_PLUGIN_ID)
            ?: error("$HELLO_COMPILER_PLUGIN_ID is not part of the build, ensure settings.gradle includes it.")

        val jarTask = pluginProject.tasks.named<Jar>("jar")
        val jarFile = jarTask.flatMap { it.archiveFile }

        project.dependencies.add(COMPILER_PLUGIN_CONFIGURATION, pluginProject)

        project.tasks.withType<KotlinCompile>().configureEach {
            dependsOn(jarTask)
            kotlinOptions.freeCompilerArgs += "-Xplugin=${jarFile.get().asFile.absolutePath}"
        }
    }
}
