/*
 * Copyright (c) 2014-2026 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core.call.components

import io.getstream.video.android.core.call.connection.Publisher
import io.getstream.video.android.core.call.connection.Subscriber
import io.getstream.video.android.core.utils.safeCall
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Sole owner of the publisher and subscriber peer connections for one RTC session.
 *
 * The two connections are read from just about everywhere — SFU events, ICE monitoring, media
 * publishing, stats, renderers, analytics — and used to be mutated from several of those places
 * too. Keeping the [MutableStateFlow]s here and handing out read-only [StateFlow]s means there is
 * one place that can swap or tear down a connection, which is what makes the surrounding code safe
 * to split apart.
 */
internal class PeerConnections {

    private val _publisher = MutableStateFlow<Publisher?>(null)
    val publisher: StateFlow<Publisher?> = _publisher.asStateFlow()

    private val _subscriber = MutableStateFlow<Subscriber?>(null)
    val subscriber: StateFlow<Subscriber?> = _subscriber.asStateFlow()

    fun setPublisher(publisher: Publisher?) {
        _publisher.value = publisher
    }

    fun setSubscriber(subscriber: Subscriber?) {
        _subscriber.value = subscriber
    }

    /**
     * Clears the subscriber's tracks, closes both connections and drops them. Safe to call more
     * than once; a session can be torn down from several paths.
     */
    fun close() {
        _subscriber.value?.clear()
        safeCall {
            _subscriber.value?.close()
            _publisher.value?.close(true)
        }
        _subscriber.value = null
        _publisher.value = null
    }
}
