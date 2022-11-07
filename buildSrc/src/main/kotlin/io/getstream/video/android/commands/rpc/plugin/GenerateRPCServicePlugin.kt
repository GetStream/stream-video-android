package io.getstream.video.android.commands.rpc.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import io.getstream.video.android.commands.rpc.task.GenerateRPCServiceTask

// import com.android.build.api.variant.AndroidComponentsExtension
// import org.gradle.configurationcache.extensions.capitalized

private const val CONFIG_CLOJURE_NAME = "generateRPCServices"
private const val COMMAND_NAME = "generateServices"

class GenerateRPCServicePlugin: Plugin<Project> {
    override fun apply(project: Project) {
        val extension: GenerateRPCServiceExtension =
            project.extensions.create(CONFIG_CLOJURE_NAME, GenerateRPCServiceExtension::class.java)

        // val androidComponents = project.extensions.getByType(AndroidComponentsExtension::class.java)

        project.tasks.create(COMMAND_NAME, GenerateRPCServiceTask::class.java) {
            extension.outputDir = "${project.rootDir.name}/build/generated/source/services/debug/stream"
            extension.srcDir = project.rootDir.name
            this.config = extension
        }

        // val variants = mutableListOf<String>()
        // androidComponents.beforeVariants { variantBuilder ->
        //     val variantName = variantBuilder.name.capitalized()
        //     variants.add(variantName)
        //     project.tasks.create("$COMMAND_NAME$variantName", GenerateRPCServiceTask::class.java) {
        //         this.config = extension
        //     }
        // }

        project.afterEvaluate {
            // variants.forEach {
                this.tasks.getByName("generateProtos").finalizedBy(project.tasks.getByName(COMMAND_NAME))
            // }
        }
    }
}