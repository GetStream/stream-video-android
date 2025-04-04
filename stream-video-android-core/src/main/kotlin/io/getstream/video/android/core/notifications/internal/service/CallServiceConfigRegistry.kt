/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
 *
 * Licensed under the Stream License;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://github.com/GetStream/stream-video-android/blob/main/LICENSE
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.getstream.video.android.core.notifications.internal.service

import io.getstream.video.android.core.call.CallType

/**
 * A registry for managing call configurations related to call services.
 *
 * #### Registering Configurations:
 *   ```kotlin
 *   val registry = CallServiceConfigRegistry()
 *
 *   // Register a configuration explicitly
 *   registry.register(CallType.Default.name, CallServiceConfig())
 *
 *   // Register a configuration using a builder
 *   registry.register("livestream") {
 *       setServiceClass(MyCallService::class.java)
 *       setRunCallServiceInForeground(true)
 *   }
 *
 *   // Register multiple configurations using a map
 *   registry.register(
 *       mapOf(
 *           CallType.AudioCall.name to CallServiceConfig(),
 *           CallType.Default.name to CallServiceConfig()
 *       )
 *   )
 *  ```
 *
 *  #### Retrieving Configurations:
 *  ```kotlin
 *  val config = registry.get(CallType.Default.name)
 *  ```
 *
 *  #### Updating Configurations:
 *  ```kotlin
 *   registry.update(CallType.Default.name) {
 *        setRunCallServiceInForeground(false)
 *   }
 *  ```
 *
 */
class CallServiceConfigRegistry {
    /**
     * A registry for managing call configurations related to call services.
     */
    private val configs = mutableMapOf<String, CallServiceConfig>()

    init {
        configs[ANY_MARKER] = DefaultCallConfigurations.default
    }

    /**
     * Registers a specific call configuration for a given call type.
     *
     * @param callType The type of call (e.g., CallType.LiveStream.name).
     * @param config The configuration to associate with the call type.
     */
    fun register(callType: String, config: CallServiceConfig) {
        configs[callType] = config
    }

    /**
     * Registers a call configuration using a builder pattern for a given call type.
     *
     * @param callType The type of call (e.g., CallType.LiveStream.name)
     * @param configure A lambda to configure the [CallServiceConfig] using [CallServiceConfigBuilder].
     *
     * Example:
     * ```kotlin
     *   registry.register(CallType.LiveStream.name) {
     *      setServiceClass(LivestreamCallService::class.java)
     *      setRunCallServiceInForeground(true)
     *   }
     * ```
     *
     */
    fun register(callType: String, configure: CallServiceConfigBuilder.() -> Unit) {
        val builder = CallServiceConfigBuilder()
        builder.configure()
        register(callType, builder.build())
    }

    /**
     * Registers multiple call configurations in bulk.
     *
     * @param map A map of [CallType] to [CallServiceConfig] representing configurations for multiple call types.
     * ```kotlin
     *   registry.register(
     *       mapOf(
     *           CallType.AudioCall.name to CallServiceConfig(),
     *           CallType.Default.name to CallServiceConfig()
     *       )
     *   )
     *  ```
     */

    fun register(map: Map<CallType, CallServiceConfig>) {
        map.forEach {
            register(it.key.name, it.value)
        }
    }

    /**
     * Configures the registry using a lambda block.
     *
     * @param block A lambda to define multiple registrations or updates to the registry.
     * ```kotlin
     *     val callServiceConfigRegistry = CallServiceConfigRegistry()
     *     callServiceConfigRegistry.createConfigRegistry {
     *          registry.register(CallType.LiveStream.name) {
     *              setServiceClass(LivestreamCallService::class.java)
     *              setRunCallServiceInForeground(true)
     *          }
     *      }
     * ```
     */
    fun createConfigRegistry(block: CallServiceConfigRegistry.() -> Unit) {
        apply(block)
    }

    /**
     * Retrieves the call configuration for the given call type.
     * Falls back to the default configuration if no specific configuration is found.
     *
     * @param callType The type of call to retrieve the configuration for.
     * @return The [CallServiceConfig] associated with the call type.
     * @throws IllegalArgumentException If no configuration is found and no default configuration is set.
     */
    fun get(callType: String): CallServiceConfig {
        return configs[callType] ?: configs[ANY_MARKER] ?: throw IllegalArgumentException("No configuration found for $callType")
    }

    /**
     * Updates an existing call configuration or creates a new one if it does not exist.
     *
     * @param callType The type of call to update the configuration for.
     * @param updater A lambda to update the [CallServiceConfig] using [CallServiceConfigBuilder].
     */
    fun update(callType: String, updater: CallServiceConfigBuilder.() -> Unit) {
        val existingConfig = configs[callType] ?: CallServiceConfig()
        register(callType) {
            setServiceClass(existingConfig.serviceClass)
            enableTelecomIntegration(existingConfig.enableTelecomIntegration)
            setRunCallServiceInForeground(existingConfig.runCallServiceInForeground)
            setAudioUsage(existingConfig.audioUsage)
            updater()
        }
    }
}
