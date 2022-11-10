package io.getstream.video.android.commands.utils

import org.gradle.api.Action
import org.gradle.api.Task
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider

inline fun <reified T : Task> TaskContainer.registerExt(
    name: String,
    configuration: Action<in T>,
): TaskProvider<T> = this.register(name, T::class.java, configuration)

