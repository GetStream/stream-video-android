package io.getstream.video.android.commands.rpc.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import io.getstream.video.android.commands.rpc.task.GenerateRPCServiceTask

/**
 * Name to configure the gradle plugin.
 */
private const val CONFIG_CLOJURE_NAME = "generateRPCServices"
private const val COMMAND_NAME = "generateServices"

/**
 * Plugin that sets up the task.
 */
class GenerateRPCServicePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension: GenerateRPCServiceExtension =
            project.extensions.create(CONFIG_CLOJURE_NAME, GenerateRPCServiceExtension::class.java)

        project.tasks.create(COMMAND_NAME, GenerateRPCServiceTask::class.java) {
            // We take the projects build dir and set the output folder inside it.
            extension.outputDir = "${project.buildDir}/generated/source/services/io/getstream/video/android/api"
            this.config = extension
        }

        project.afterEvaluate {
            this.tasks.getByName("preBuild").finalizedBy(project.tasks.getByName(COMMAND_NAME))
        }
    }
}