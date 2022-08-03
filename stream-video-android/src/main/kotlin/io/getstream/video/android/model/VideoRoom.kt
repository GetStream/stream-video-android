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

import io.getstream.video.android.events.AudioMutedEvent
import io.getstream.video.android.events.AudioUnmutedEvent
import io.getstream.video.android.events.ParticipantJoinedEvent
import io.getstream.video.android.events.ParticipantLeftEvent
import io.getstream.video.android.events.VideoEvent
import io.getstream.video.android.events.VideoStartedEvent
import io.getstream.video.android.events.VideoStoppedEvent
import io.getstream.video.android.socket.SocketListener
import io.getstream.video.android.socket.VideoSocket
import io.getstream.video.android.utils.update
import io.getstream.video.android.utils.updateAll
import io.livekit.android.room.Room
import io.livekit.android.room.track.CameraPosition
import io.livekit.android.room.track.LocalVideoTrack
import io.livekit.android.room.track.LocalVideoTrackOptions
import io.livekit.android.room.track.Track
import io.livekit.android.util.flow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

public data class VideoRoom(
    public val value: Room,
    public val videoSocket: VideoSocket
) : SocketListener {
    public val localParticipant: VideoParticipant by lazy { VideoParticipant(value.localParticipant) }

    private val _participants: MutableStateFlow<List<VideoParticipant>> =
        MutableStateFlow(emptyList())
    public val participants: StateFlow<List<VideoParticipant>> = _participants

    private val _participantsState: MutableStateFlow<List<VideoParticipantState>> =
        MutableStateFlow(emptyList())

    public val participantsState: StateFlow<List<VideoParticipantState>> = _participantsState

    public val activeSpeakers: Flow<List<VideoParticipant>>
        get() = value::activeSpeakers.flow.map { list ->
            list.map { VideoParticipant(participant = it) }
        }

    public val remoteParticipants: Flow<Pair<Set<String>, List<VideoParticipant>>>
        get() = value::remoteParticipants.flow.map { map ->
            val ids = map.keys
            val values = map.values

            ids to values.map { VideoParticipant(it) }
        }

    public suspend fun connect(url: String, token: String) {
        videoSocket.addListener(this)
        value.connect(url, token)
    }

    public fun updateParticipants(list: List<stream.video.Participant>) {
        val onlineParticipants = list.filter { it.online }

        this._participants.value =
            onlineParticipants.map { VideoParticipant(streamParticipant = it) }
        this._participantsState.value = onlineParticipants.mapNotNull { participant ->
            participant.user?.let { user ->
                // TODO - when initializing we'll have to pull data from the BE to sync up, right now it's coming from multiple sources
                VideoParticipantState(
                    userId = user.id,
                    userName = user.name,
                    isLocalAudioEnabled = true, // participant.audio
                    isLocalVideoEnabled = true // participant.video
                )
            }
        }
    }

    public fun disconnect() {
        videoSocket.removeListener(this)
        value.disconnect()
    }

    override fun onEvent(event: VideoEvent) {
        super.onEvent(event)

        when (event) {
            is ParticipantJoinedEvent -> {
                val participants = _participants.value
                val participantsState = _participantsState.value
                val user = event.participant.user

                if (user != null) {
                    _participants.value =
                        (participants + VideoParticipant(streamParticipant = event.participant)).distinctBy { it.user?.id }

                    _participantsState.value =
                        (
                            participantsState + VideoParticipantState(
                                userId = user.id,
                                userName = user.name,
                                isLocalAudioEnabled = true,
                                isLocalVideoEnabled = true
                            )
                            ).distinctBy { it.userId }
                }
            }
            is ParticipantLeftEvent -> {
                val participants = _participants.value
                val participantsState = _participantsState.value

                _participants.value =
                    participants.filter { it.user?.id != event.participant.user?.id }

                _participantsState.value =
                    participantsState.filter { it.userId != event.participant.user?.id }
            }

            is AudioMutedEvent -> {
                if (event.areAllUsersMuted) {
                    muteAllUsers()
                } else {
                    val updatedData = getParticipants().update(
                        condition = { it.userId == event.userId },
                        transformer = { it.copy(isLocalAudioEnabled = false) }
                    )

                    refreshData(updatedData)
                }
            }

            is AudioUnmutedEvent -> {
                val updatedData = getParticipants().update(
                    condition = { it.userId == event.userId },
                    transformer = { it.copy(isLocalAudioEnabled = true) }
                )

                refreshData(updatedData)
            }

            is VideoStoppedEvent -> {
                val updatedData = getParticipants().update(
                    condition = { it.userId == event.userId },
                    transformer = { it.copy(isLocalVideoEnabled = false) }
                )

                refreshData(updatedData)
            }

            is VideoStartedEvent -> {
                val updatedData = getParticipants().update(
                    condition = { it.userId == event.userId },
                    transformer = { it.copy(isLocalVideoEnabled = true) }
                )

                refreshData(updatedData)
            }

            else -> Unit
        }
    }

    private fun getParticipants(): List<VideoParticipantState> {
        return _participantsState.value
    }

    private fun muteAllUsers() {
        val users = getParticipants().updateAll { it.copy(isLocalAudioEnabled = false) }
        refreshData(users)
    }

    private fun refreshData(data: List<VideoParticipantState>) {
        _participantsState.value = data
    }

    public fun flipCamera() {
        val videoTrack = localParticipant.getTrackPublication(Track.Source.CAMERA)
            ?.track as? LocalVideoTrack
            ?: return

        val newOptions = when (videoTrack.options.position) {
            CameraPosition.FRONT -> LocalVideoTrackOptions(position = CameraPosition.BACK)
            CameraPosition.BACK -> LocalVideoTrackOptions(position = CameraPosition.FRONT)
            else -> LocalVideoTrackOptions()
        }

        videoTrack.restartTrack(newOptions)
    }

    public fun addEventListener(socketListener: SocketListener) {
        videoSocket.addListener(socketListener = socketListener)
    }

    public fun removeEventListener(socketListener: SocketListener) {
        videoSocket.removeListener(socketListener)
    }
}
