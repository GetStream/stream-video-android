/*
 * Copyright (c) 2014-2022 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.model

import android.content.Context
import android.media.AudioManager
import android.util.Log
import androidx.core.content.getSystemService
import io.getstream.video.android.audio.AudioHandler
import io.getstream.video.android.audio.AudioSwitchHandler
import io.getstream.video.android.events.MuteStateChangeEvent
import io.getstream.video.android.events.SfuParticipantJoinedEvent
import io.getstream.video.android.events.SfuParticipantLeftEvent
import io.getstream.video.android.token.CredentialsProvider
import io.getstream.video.android.utils.updateValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import org.webrtc.EglBase
import org.webrtc.MediaStream
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack
import stream.video.sfu.CallState

public class Room(
    private val context: Context,
    private val sessionId: String,
    private val credentialsProvider: CredentialsProvider,
    private val eglBase: EglBase,
) {

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    private val audioHandler: AudioHandler by lazy {
        AudioSwitchHandler(context)
    }

    private val _callParticipants: MutableStateFlow<List<CallParticipant>> =
        MutableStateFlow(emptyList())

    public val callParticipants: StateFlow<List<CallParticipant>> = _callParticipants

    public val callParticipantState: Flow<List<CallParticipantState>>
        get() = callParticipants.map { list ->
            list.map {
                CallParticipantState(
                    it.id,
                    it.name,
                    it.hasAudio,
                    it.hasVideo
                )
            }
        }

    private val audioManager by lazy {
        context.getSystemService<AudioManager>()
    }

    public var onLocalVideoTrackChange: (VideoTrack) -> Unit = {}
    public var onStreamAdded: (MediaStream) -> Unit = {}
    public var onStreamRemoved: (MediaStream) -> Unit = {}

    /**
     * Public API.
     */
    public fun initRenderer(videoRenderer: SurfaceViewRenderer) {
        videoRenderer.init(eglBase.eglBaseContext, null)
    }

    internal fun addStream(mediaStream: MediaStream) {
        if (mediaStream.audioTracks.isNotEmpty()) {
            Log.d("sfuConnectFlow", "AudioTracks received, ${mediaStream.audioTracks}")
            mediaStream.audioTracks.forEach { it.setEnabled(true) }

            audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager?.isSpeakerphoneOn = true
        }

        Log.d("sfuConnectFlow", "StreamAdded, $mediaStream")

        val updatedList = _callParticipants.value.updateValue(
            predicate = { it.id in mediaStream.id },
            transformer = {
                it.copy(
                    track = mediaStream.videoTracks?.firstOrNull()
                )
            }
        )
        _callParticipants.value = updatedList
        onStreamAdded(mediaStream)
    }

    internal fun removeStream(mediaStream: MediaStream) {
        Log.d("sfuConnectFlow", "StreamRemoved, $mediaStream")
        val updatedList = _callParticipants.value.updateValue(
            predicate = { it.id in mediaStream.id },
            transformer = {
                it.copy(track = null, trackSize = 0 to 0)
            }
        )

        _callParticipants.value = updatedList
        onStreamRemoved(mediaStream)
    }

    internal fun loadParticipants(callState: CallState) {
        this._callParticipants.value =
            callState.participants.map { it.toCallParticipant(credentialsProvider.getUserCredentials().id) }

        Log.d("sfuConnectFlow", "ExecuteJoin, $_callParticipants")
    }

    internal fun updateMuteState(event: MuteStateChangeEvent) {
        val currentParticipants = _callParticipants.value

        val updatedList = currentParticipants.updateValue(
            predicate = { it.id == event.userId },
            transformer = {
                it.copy(
                    hasAudio = !event.audioMuted,
                    hasVideo = !event.videoMuted
                )
            }
        )

        _callParticipants.value = updatedList
    }

    internal fun addParticipant(event: SfuParticipantJoinedEvent) {
        _callParticipants.value =
            _callParticipants.value + event.participant.toCallParticipant(credentialsProvider.getUserCredentials().id)
    }

    internal fun removeParticipant(event: SfuParticipantLeftEvent) {
        val userId = event.participant.user?.id ?: return

        _callParticipants.value = _callParticipants.value.filter { it.id != userId }
    }

    internal fun updateLocalVideoTrack(localVideoTrack: VideoTrack) {
        val updatedParticipants = _callParticipants.value.updateValue(
            predicate = { it.id == credentialsProvider.getUserCredentials().id },
            transformer = { it.copy(track = localVideoTrack) }
        )

        _callParticipants.value = updatedParticipants
        onLocalVideoTrackChange(localVideoTrack)
    }

    internal fun startAudio() {
        audioHandler.start()
    }

    internal fun disconnect() {
        audioHandler.stop()
        _callParticipants.value = emptyList()
    }
}
