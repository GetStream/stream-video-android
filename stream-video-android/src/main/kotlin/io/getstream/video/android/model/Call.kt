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
import android.media.AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
import android.media.AudioManager
import android.os.Build
import android.view.View
import androidx.core.content.getSystemService
import io.getstream.logging.StreamLog
import io.getstream.video.android.audio.AudioHandler
import io.getstream.video.android.audio.AudioSwitchHandler
import io.getstream.video.android.events.MuteStateChangeEvent
import io.getstream.video.android.events.SfuParticipantJoinedEvent
import io.getstream.video.android.events.SfuParticipantLeftEvent
import io.getstream.video.android.token.CredentialsProvider
import io.getstream.video.android.utils.updateValue
import io.getstream.video.android.webrtc.utils.stringify
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import org.webrtc.AudioTrack
import org.webrtc.EglBase
import org.webrtc.MediaStream
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import stream.video.sfu.CallState

public class Call(
    private val context: Context,
    public val sessionId: String,
    private val credentialsProvider: CredentialsProvider,
    private val eglBase: EglBase,
) {

    private val logger = StreamLog.getLogger("Call:Room")

    internal val audioHandler: AudioHandler by lazy {
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

    private val _localParticipant: MutableStateFlow<CallParticipant?> = MutableStateFlow(null)

    public val localParticipant: Flow<CallParticipant> =
        _localParticipant.filterNotNull()

    public val localParticipantId: String
        get() = credentialsProvider.getUserCredentials().id

    private val audioManager by lazy {
        context.getSystemService<AudioManager>()
    }

    private val remoteAudioTracks = mutableSetOf<AudioTrack>()

    public var onStreamAdded: (MediaStream) -> Unit = {}
    public var onStreamRemoved: (MediaStream) -> Unit = {}

    /**
     * Public API.
     */
    public fun initRenderer(
        videoRenderer: SurfaceViewRenderer,
        streamId: String,
        onRender: (View) -> Unit = {}
    ) {
        logger.d { "[initRenderer] #sfu; streamId: $streamId" }
        videoRenderer.init(
            eglBase.eglBaseContext,
            object : RendererCommon.RendererEvents {
                override fun onFirstFrameRendered() {
                    logger.v { "[initRenderer.onFirstFrameRendered] #sfu; streamId: $streamId" }
                    updateParticipantTrackSize(
                        streamId,
                        videoRenderer.measuredWidth,
                        videoRenderer.measuredHeight
                    )
                    onRender(videoRenderer)
                }

                override fun onFrameResolutionChanged(p0: Int, p1: Int, p2: Int) {
                    logger.v { "[initRenderer.onFrameResolutionChanged] #sfu; streamId: $streamId" }
                }
            }
        )
    }

    private fun updateParticipantTrackSize(
        streamId: String,
        measuredWidth: Int,
        measuredHeight: Int
    ) {
        val oldState = _callParticipants.value

        val newState = oldState.updateValue(
            predicate = { it.id in streamId },
            transformer = { it.copy(trackSize = measuredWidth to measuredHeight) }
        )

        _callParticipants.value = newState
    }

    internal fun addStream(mediaStream: MediaStream) {
        logger.i { "[addStream] #sfu; mediaStream: $mediaStream" }
        if (mediaStream.audioTracks.isNotEmpty()) {
            mediaStream.audioTracks.forEach { track ->
                logger.v { "[addStream] #sfu; audioTrack: ${track.stringify()}" }
                track.setEnabled(true)
                track.setVolume(15.0)
            }

            remoteAudioTracks.addAll(mediaStream.audioTracks)
            logger.d { "[addStream] #sfu; remoteAudioTracks: $remoteAudioTracks" }

            updateAudio()
        }

        val updatedList = _callParticipants.value.updateValue(
            predicate = { it.id in mediaStream.id },
            transformer = {
                val track = replaceTrackIfNeeded(mediaStream, it.track?.streamId)

                if (track != null) {
                    it.copy(track = track)
                } else {
                    it
                }
            }
        )
        _callParticipants.value = updatedList
        onStreamAdded(mediaStream)
    }

    private fun updateAudio() {
        val switchHandler = audioHandler as? AudioSwitchHandler
        if (switchHandler != null && switchHandler.availableAudioDevices.isNotEmpty()) {
            val speaker = switchHandler.availableAudioDevices.firstOrNull {
                it.name.contains(
                    "speaker",
                    true
                )
            }

            if (speaker != null) {
                switchHandler.selectDevice(speaker)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val devices = audioManager?.availableCommunicationDevices ?: return
            val speaker = devices.firstOrNull { it.type == TYPE_BUILTIN_SPEAKER } ?: return

            val isCommunicationDeviceSet = audioManager?.setCommunicationDevice(speaker)
            logger.d { "[updateAudio] #sfu; isCommunicationDeviceSet: $isCommunicationDeviceSet" }
        }
        audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager?.isSpeakerphoneOn = true
    }

    private fun replaceTrackIfNeeded(mediaStream: MediaStream, streamId: String?): VideoTrack? {
        if (streamId == null || streamId != mediaStream.id) {
            return mediaStream.videoTracks?.firstOrNull()?.let { track ->
                VideoTrack(
                    streamId = mediaStream.id,
                    video = track
                )
            }
        } else {
            return null
        }
    }

    internal fun removeStream(mediaStream: MediaStream) {
        logger.d { "[removeStream] #sfu; mediaStream: $mediaStream" }
        val updatedList = _callParticipants.value.updateValue(
            predicate = { it.id in mediaStream.id },
            transformer = {
                it.copy(track = null, trackSize = 0 to 0)
            }
        )

        _callParticipants.value = updatedList
        onStreamRemoved(mediaStream)
    }

    internal fun loadParticipants(callState: CallState, callSettings: CallSettings) {
        val allParticipants =
            callState.participants.map {
                val user = it.toCallParticipant(credentialsProvider.getUserCredentials().id)

                if (it.user?.id == credentialsProvider.getUserCredentials().id) {
                    user.hasAudio = callSettings.audioOn
                    user.hasVideo = callSettings.videoOn
                }

                user
            }

        this._callParticipants.value = allParticipants
        logger.d { "[loadParticipants] #sfu; allParticipants: ${_callParticipants.value}" }

        this._localParticipant.value = allParticipants.firstOrNull { it.isLocal }
        logger.v { "[loadParticipants] #sfu; localParticipants: ${_localParticipant.value}" }
    }

    internal fun updateMuteState(event: MuteStateChangeEvent) {
        logger.d { "[updateMuteState] #sfu; event: $event" }
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
        logger.d { "[addParticipant] #sfu; event: $event" }
        _callParticipants.value =
            _callParticipants.value + event.participant.toCallParticipant(credentialsProvider.getUserCredentials().id)
    }

    internal fun removeParticipant(event: SfuParticipantLeftEvent) {
        logger.d { "[removeParticipant] #sfu; event: $event" }
        val userId = event.participant.user?.id ?: return

        _callParticipants.value = _callParticipants.value.filter { it.id != userId }
    }

    internal fun updateLocalVideoTrack(localVideoTrack: org.webrtc.VideoTrack) {
        logger.d { "[updateLocalVideoTrack] #sfu; localVideoTrack: $localVideoTrack" }
        val localParticipant = _localParticipant.value ?: return
        val allParticipants = callParticipants.value

        val videoTrack = VideoTrack(
            video = localVideoTrack,
            streamId = "${credentialsProvider.getUserCredentials().id}:${localVideoTrack.id()}"
        )

        val updatedParticipant = localParticipant.copy(
            track = videoTrack
        )

        val updated = allParticipants.updateValue(
            predicate = { it.id == credentialsProvider.getUserCredentials().id },
            transformer = {
                it.copy(track = videoTrack)
            }
        )

        _localParticipant.value = updatedParticipant
        _callParticipants.value = updated
    }

    internal fun setupAudio() {
        logger.d { "[setupAudio] #sfu; no args" }
        audioHandler.start()
    }

    internal fun disconnect() {
        logger.d { "[disconnect] #sfu; no args" }
        audioHandler.stop()
        _callParticipants.value.forEach { it.track?.video?.dispose() }
        _callParticipants.value = emptyList()
    }

    public fun getLocalParticipant(): CallParticipant? {
        return _callParticipants.value.firstOrNull { it.isLocal }
    }

    // TODO - check if this is needed or if we can rely on the MuteEvent
    public fun setCameraEnabled(isEnabled: Boolean) {
        logger.d { "[setCameraEnabled] #sfu; isEnabled: $isEnabled" }
        val localParticipant = _localParticipant.value
        val updatedLocal = localParticipant?.copy(hasVideo = isEnabled)
        _localParticipant.value = updatedLocal

        val updatedList = _callParticipants.value.updateValue(
            predicate = { it.id == credentialsProvider.getUserCredentials().id },
            transformer = { it.copy(hasVideo = isEnabled) }
        )

        _callParticipants.value = updatedList
    }

    public fun setMicrophoneEnabled(isEnabled: Boolean) {
        logger.d { "[setMicrophoneEnabled] #sfu; isEnabled: $isEnabled" }
        val localParticipant = _localParticipant.value
        val updatedLocal = localParticipant?.copy(hasAudio = isEnabled)
        _localParticipant.value = updatedLocal

        val updatedList = _callParticipants.value.updateValue(
            predicate = { it.id == credentialsProvider.getUserCredentials().id },
            transformer = { it.copy(hasAudio = isEnabled) }
        )

        _callParticipants.value = updatedList
    }
}
