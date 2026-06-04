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

import io.getstream.log.taggedLogger
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.events.reporting.ClientEventReporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import org.webrtc.AudioTrackSink
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

private typealias TrackId = String
internal class AudioAnalytics(
    private val callId: String,
    private val callType: String,
    private val clientEventReporter: ClientEventReporter,
    private val onSfuId: () -> String,
    val getJoinStageAttemptId: () -> String,
) {

    val logger by taggedLogger("AudioAnalytics")
    var recordedFirstFrame: AtomicBoolean = AtomicBoolean(false)

    private val trackSinks =
        ConcurrentHashMap<TrackId, Pair<org.webrtc.AudioTrack, AudioTrackSink>>()
    private var observeJob: Job? = null

    fun observeParticipantsForFirstRemoteAudioFrame(
        participants: StateFlow<List<ParticipantState>>,
        scope: CoroutineScope,
    ) {
        observeJob?.cancel()
        observeJob = scope.launch {
            participants
                .flatMapLatest { list ->
                    val audioTrackFlows = list
                        .filter { !it.isLocal }
                        .map { it.audioTrack.filterNotNull() }
                    if (audioTrackFlows.isEmpty()) {
                        emptyFlow()
                    } else {
                        merge(*audioTrackFlows.toTypedArray())
                    }
                }
                .collect { modelAudioTrack ->
                    if (trackSinks.containsKey(modelAudioTrack.streamId)) return@collect
                    val webRtcTrack = modelAudioTrack.audio
                    val sink =
                        AudioTrackSink { audioData, bitsPerSample, sampleRate, numberOfChannels, numberOfFrames, _ ->
                            // onData fires on the WebRTC native audio thread every ~10ms.
                            // Rules:
                            //  - No logging (I/O on a real-time thread)
                            //  - No removeSink (deadlocks the sink-list lock WebRTC holds here)
                            //  - No blocking calls
                            // CAS here so exactly ONE coroutine is ever launched, then
                            // hand off all real work (reporting + cleanup) to the coroutine.
                            if (numberOfFrames > 0 && sampleRate > 0 && numberOfChannels > 0 && audioData.hasRemaining()) {
                                scope.launch {
                                    if (recordedFirstFrame.compareAndSet(false, true)) {
                                        reportAndCleanup()
                                    }
                                }
                            }
                        }
                    trackSinks[modelAudioTrack.streamId] = Pair(webRtcTrack, sink)
                    webRtcTrack.addSink(sink)
                }
        }
    }

    // Called from a coroutine — safe to do I/O, removeSink, and job cancellation here.
    private fun reportAndCleanup() {
        clientEventReporter.reportFirstAudioFrameRendered(
            onSfuId(),
            callId,
            callType,
            getJoinStageAttemptId(),
        )
        trackSinks.forEach { (_, pair) -> pair.first.removeSink(pair.second) }
        trackSinks.clear()
        observeJob?.cancel()
        observeJob = null
    }

    fun reset() {
        logger.d { "noob [reset]" }
        recordedFirstFrame.set(false)
        trackSinks.forEach { (_, pair) -> pair.first.removeSink(pair.second) }
        trackSinks.clear()
        observeJob?.cancel()
        observeJob = null
    }
}
