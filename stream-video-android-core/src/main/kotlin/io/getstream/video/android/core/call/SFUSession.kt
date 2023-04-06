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

package io.getstream.video.android.core.call

import io.getstream.result.Result
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.call.signal.socket.SfuSocketListener
import io.getstream.video.android.core.model.CallSettings
import kotlinx.coroutines.flow.StateFlow
import org.webrtc.RTCStatsReport

public interface SFUSession {

    /**
     * State that indicates whether the camera is capturing and sending video or not.
     */
    public val isVideoEnabled: StateFlow<Boolean>

    /**
     * State that indicates whether the mic is capturing and sending the audio or not.
     */
    public val isAudioEnabled: StateFlow<Boolean>

    /**
     * State that indicates whether the speakerphone is on or not.
     */
    public val isSpeakerPhoneEnabled: StateFlow<Boolean>

    public fun clear()

    public suspend fun connectToCall(
        sessionId: String,
        autoPublish: Boolean
    ): Result<Call>

    /**
     * @return The active call instance, if it exists.
     */
    public fun getActiveCall(): Call?

    /**
     * Exposes the publisher stats for the connection, if they exist.
     */
    public fun getPublisherStats(): StateFlow<RTCStatsReport?>

    /**
     * Exposes the subscriber stats for the connection, if they exist.
     */
    public fun getSubscriberStats(): StateFlow<RTCStatsReport?>

    /**
     * Sets the initial state of media for the CallClient after it has been constructed so it is up to date with the
     * UI state.
     */
    public fun setInitialCallSettings(callSettings: CallSettings)

    public fun setCameraEnabled(isEnabled: Boolean)

    public fun setMicrophoneEnabled(isEnabled: Boolean)

    public fun setSpeakerphoneEnabled(isEnabled: Boolean)

    public fun flipCamera()

    public fun selectAudioDevice(device: io.getstream.video.android.core.audio.AudioDevice)

    public fun addSocketListener(sfuSocketListener: SfuSocketListener)

    public fun removeSocketListener(sfuSocketListener: SfuSocketListener)
}
