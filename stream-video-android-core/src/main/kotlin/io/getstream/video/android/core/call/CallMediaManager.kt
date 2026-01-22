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

package io.getstream.video.android.core.call

import io.getstream.android.video.generated.models.AudioSettingsResponse
import io.getstream.android.video.generated.models.CallSettingsResponse
import io.getstream.android.video.generated.models.VideoSettingsResponse
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.CameraDirection
import io.getstream.video.android.core.DeviceStatus
import io.getstream.video.android.core.audio.StreamAudioDevice
import io.getstream.video.android.core.model.AudioTrack
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import stream.video.sfu.models.TrackType
import kotlin.getValue

internal class CallMediaManager(private val call: Call) {

    private val logger by taggedLogger("CallMediaManager")

    /**
     * Enables or disables the reception of incoming audio tracks for all or specified participants.
     *
     * This method allows selective control over whether the local client receives audio from remote participants.
     * It's particularly useful in scenarios such as livestreams or group calls where the user may want to mute
     * specific participants' audio without affecting the overall session.
     *
     * @param enabled `true` to enable (subscribe to) incoming audio, `false` to disable (unsubscribe from) it.
     * @param sessionIds Optional list of participant session IDs for which to toggle incoming audio.
     * If `null`, the audio setting is applied to all participants currently in the session.
     */
    internal fun setIncomingAudioEnabled(
        session: RtcSession?,
        enabled: Boolean,
        sessionIds: List<String>? = null,
    ) {
        val participantTrackMap = session?.subscriber?.tracks ?: return

        val targetTracks = when {
            sessionIds != null -> sessionIds.mapNotNull { participantTrackMap[it] }
            else -> participantTrackMap.values.toList()
        }

        targetTracks
            .mapNotNull { it[TrackType.TRACK_TYPE_AUDIO] as? AudioTrack }
            .forEach { it.enableAudio(enabled) }
    }

    internal fun updateMediaManagerFromSettings(callSettings: CallSettingsResponse) {
        // Speaker
        if (call.speaker.status.value is DeviceStatus.NotSelected) {
            val enableSpeaker =
                if (callSettings.video.cameraDefaultOn || call.camera.status.value is DeviceStatus.Enabled) {
                    // if camera is enabled then enable speaker. Eventually this should
                    // be a new audio.defaultDevice setting returned from backend
                    true
                } else {
                    callSettings.audio.defaultDevice == AudioSettingsResponse.DefaultDevice.Speaker ||
                        callSettings.audio.speakerDefaultOn
                }

            call.speaker.setEnabled(enabled = enableSpeaker)
        }

        monitorHeadset()

        // Camera
        if (call.camera.status.value is DeviceStatus.NotSelected) {
            val defaultDirection =
                if (callSettings.video.cameraFacing == VideoSettingsResponse.CameraFacing.Front) {
                    CameraDirection.Front
                } else {
                    CameraDirection.Back
                }
            call.camera.setDirection(defaultDirection)
            call.camera.setEnabled(callSettings.video.cameraDefaultOn)
        }

        // Mic
        if (call.microphone.status.value == DeviceStatus.NotSelected) {
            val enabled = callSettings.audio.micDefaultOn
            call.microphone.setEnabled(enabled)
        }
    }

    private fun monitorHeadset() {
        call.microphone.devices.onEach { availableDevices ->
            logger.d {
                "[monitorHeadset] new available devices, prev selected: ${call.microphone.nonHeadsetFallbackDevice}"
            }

            val bluetoothHeadset =
                availableDevices.find { it is StreamAudioDevice.BluetoothHeadset }
            val wiredHeadset = availableDevices.find { it is StreamAudioDevice.WiredHeadset }

            if (bluetoothHeadset != null) {
                logger.d { "[monitorHeadset] BT headset selected" }
                call.microphone.select(bluetoothHeadset)
            } else if (wiredHeadset != null) {
                logger.d { "[monitorHeadset] wired headset found" }
                call.microphone.select(wiredHeadset)
            } else {
                logger.d { "[monitorHeadset] no headset found" }

                call.microphone.nonHeadsetFallbackDevice?.let { deviceBeforeHeadset ->
                    logger.d { "[monitorHeadset] before device selected" }
                    call.microphone.select(deviceBeforeHeadset)
                }
            }
        }.launchIn(call.scope)
    }
}
