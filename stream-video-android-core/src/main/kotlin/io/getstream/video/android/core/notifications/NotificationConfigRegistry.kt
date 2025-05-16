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

package io.getstream.video.android.core.notifications

import io.getstream.video.android.core.call.CallType

class NotificationConfigRegistry {

    private val configs: MutableMap<String, CallNotificationConfig> =
        HashMap(DefaultCallNotificationConfigs.all)

    fun register(callType: String, config: CallNotificationConfig) {
        configs[callType] = config
    }

    fun register(callType: String, configBuilder: () -> CallNotificationConfig) {
        configs[callType] = configBuilder()
    }

    fun registerAll(map: Map<String, CallNotificationConfig>) {
        map.forEach { (type, config) ->
            configs[type] = config
        }
    }

    fun get(callType: String): CallNotificationConfig {
        return configs[callType]
            ?: configs[CallType.AnyMarker.name]
            ?: CallNotificationConfig() // fully empty fallback
    }

    fun update(callType: String, updater: CallNotificationConfig.() -> CallNotificationConfig) {
        val current = configs[callType] ?: CallNotificationConfig()
        configs[callType] = current.updater()
    }

    fun createConfigRegistry(block: NotificationConfigRegistry.() -> Unit) {
        apply(block)
    }
}
