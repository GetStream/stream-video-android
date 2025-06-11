package io.getstream.video.android.core.trace

import java.util.concurrent.ConcurrentHashMap

/**
 * Factory that provides [Tracer] instances.
 */
class TracerFactory(private var enabled: Boolean = true) {
    /**
     * All tracers created by this factory.
     */
    private val tracers = ConcurrentHashMap<String, Tracer>()

    fun tracers(): List<Tracer> {
        return tracers.values.toList()
    }

    /**
     * Returns a [Tracer] for the given [name].
     *
     * @param name The name of the tracer.
     * @return [Tracer] for the given [name].
     */
    fun tracer(name: String): Tracer {
        return tracers.getOrPut(name) { Tracer(name).also { it.setEnabled(enabled) } }
    }

    /** Clears all tracers. */
    fun clear() {
        tracers.clear()
    }

    /**
     * Enables or disables tracing.
     *
     * @param enabled True if tracing should be enabled, false otherwise.
     */
    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        tracers.values.forEach { it.setEnabled(enabled) }
    }
}
