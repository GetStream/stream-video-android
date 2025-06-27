package io.getstream.video.android.core.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Config for the [TaskSchedulerWithDebounce] to schedule a task.
 *
 * @param debounce The debounce time in milliseconds.
 */
internal data class ScheduleConfig(
    val debounce: () -> Long = { 0 }
)

/**
 * Helper class to schedule a task with debounce.
 */
internal class TaskSchedulerWithDebounce() {
    private var job: Job? = null

    /**
     * Schedule a task with debounce.
     *
     * @param scope The coroutine scope to launch the task.
     * @param config The config for the task.
     * @param block The task to be executed.
     */
    fun schedule(scope: CoroutineScope, config: ScheduleConfig, block: () -> Unit) {
        job?.cancel()
        val delay: Long = config.debounce()
        if (delay <= 0L) {
            block()
            return
        } else {
            job = scope.launch(Dispatchers.Unconfined) {
                delay(delay)
                block()
            }
        }
    }
}