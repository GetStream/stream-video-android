/*
 * Copyright (c) 2014-2026 Stream.io Inc. All rights reserved.
 *
 * Licensed under the Stream License; see the repository LICENSE file.
 */
package io.getstream.gradle.audioswitch

import com.android.build.api.artifact.ScopedArtifact
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.ScopedArtifacts
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

public class AudioSwitchPatchPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        var configured = false

        project.pluginManager.withPlugin("com.android.library") {
            throw GradleException(
                "The io.getstream.audioswitch-patch plugin must be applied to the final " +
                    "Android application, not an Android library.",
            )
        }

        project.pluginManager.withPlugin("com.android.application") {
            configured = true
            val androidComponents = project.extensions.getByType(
                ApplicationAndroidComponentsExtension::class.java,
            )

            androidComponents.onVariants { variant ->
                val variantName = variant.name.replaceFirstChar { character ->
                    if (character.isLowerCase()) character.titlecase() else character.toString()
                }
                val patchTask = project.tasks.register(
                    "patch${variantName}AudioSwitch",
                    ReplaceAudioSwitchClassesTask::class.java,
                ) { task ->
                    task.patchId.set(PATCH_ID)
                }

                variant.artifacts
                    .forScope(ScopedArtifacts.Scope.ALL)
                    .use(patchTask)
                    .toTransform(
                        ScopedArtifact.CLASSES,
                        ReplaceAudioSwitchClassesTask::allJars,
                        ReplaceAudioSwitchClassesTask::allDirectories,
                        ReplaceAudioSwitchClassesTask::output,
                    )
            }
        }

        project.afterEvaluate {
            if (!configured) {
                throw GradleException(
                    "The io.getstream.audioswitch-patch plugin requires " +
                        "com.android.application.",
                )
            }
        }
    }

    private companion object {
        private const val PATCH_ID = "audioswitch-1.2.0-getstream-1"
    }
}
