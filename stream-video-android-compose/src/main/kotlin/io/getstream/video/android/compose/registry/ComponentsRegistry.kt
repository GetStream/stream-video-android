package io.getstream.video.android.compose.registry

import android.content.res.Resources.NotFoundException
import androidx.compose.runtime.Composable
import coil.ComponentRegistry
import io.getstream.log.taggedLogger

@DslMarker
public annotation class StreamRegistrationMarker

public annotation class Component(val value: String)

public object ComponentsRegistry {

    private val logger by taggedLogger("ComponentsRegistry")
    private val componentsMap : MutableMap<String, @Composable () -> Unit> = mutableMapOf()

    @StreamRegistrationMarker
    public fun register(key: String, composable: @Composable () -> Unit) {
        componentsMap[key] = composable
    }

    @StreamRegistrationMarker
    public fun component(key: String) : @Composable () -> Unit = {
        if (componentsMap.containsKey(key)) {
            componentsMap[key]!!
        } else {
            val e = NotFoundException("Component under key::$key not found. Register id using ComponentRegistry.register")
            logger.e(e) { "Component not found." }
            throw e
        }
    }
}

public class RegisterComponentData(
    public val key: String,
    public val composable: @Composable () -> Unit
)

public inline fun componentRegistration(registration: ComponentsRegistry.() -> Unit): Unit = registration(ComponentsRegistry)