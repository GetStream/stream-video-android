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

package io.getstream.video.android.core.analytics

import io.getstream.video.android.core.call.RtcSession
import io.getstream.video.android.core.events.reporting.PeerConnectionRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

internal class PeerConnectionAnalyticsObserver(
    private val scope: CoroutineScope,
    private val hook: PeerConnectionHook,
) {

    private var peerConnectionObserverJob: Job? = null

    fun observePeerConnections(session: StateFlow<RtcSession?>) {
        peerConnectionObserverJob?.cancel()
        peerConnectionObserverJob = scope.launch {
            launch {
                session.filterNotNull()
                    .flatMapLatest { it.publisher.filterNotNull() }
                    .flatMapLatest { it.state.filterNotNull() }
                    .collect { state ->
                        hook.onPeerConnectionStateChanged(
                            role = PeerConnectionRole.PUBLISH,
                            iceState = session.value?.publisher?.value?.iceState?.value,
                            peerConnectionState = state,
                        )
                    }

                session.filterNotNull()
                    .flatMapLatest { it.subscriber.filterNotNull() }
                    .flatMapLatest { it.state.filterNotNull() }
                    .collect { state ->
                        hook.onPeerConnectionStateChanged(
                            role = PeerConnectionRole.SUBSCRIBE,
                            iceState = session.value?.subscriber?.value?.iceState?.value,
                            peerConnectionState = state,
                        )
                    }
            }
        }
    }

    fun stop() {
        peerConnectionObserverJob?.cancel()
        peerConnectionObserverJob = null
    }
}
