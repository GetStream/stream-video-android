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

package io.getstream.video.android.core.notifications.internal.service.observers

import android.content.Context
import android.media.AudioManager
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.RingingState
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.model.RejectReason
import io.getstream.video.android.core.socket.common.scope.ClientScope
import io.getstream.video.android.core.sounds.CallSoundAndVibrationPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class CallServiceRingingStateObserver(
    private val call: Call,
    private val soundPlayer: CallSoundAndVibrationPlayer?,
    private val streamVideo: StreamVideoClient,
    private val scope: CoroutineScope,
) {
    private val logger by taggedLogger("RingingStateObserver")

    /**
     * Starts observing ringing state changes.
     */
    fun observe(onStopService: () -> Unit) {
        call.scope.launch {
            call.state.ringingState.collect { state ->
                logger.i { "Ringing state: $state" }
                handleRingingState(state, onStopService)
            }
        }
    }

    /**
     * Handles different ringing states.
     */
    private fun handleRingingState(state: RingingState, onStopService: () -> Unit) {
        when (state) {
            is RingingState.Incoming -> handleIncomingState(state)
            is RingingState.Outgoing -> handleOutgoingState(state)
            is RingingState.Active -> handleActiveState()
            is RingingState.RejectedByAll -> handleRejectedByAllState(onStopService)
            is RingingState.TimeoutNoAnswer -> handleTimeoutState()
            else -> soundPlayer?.stopCallSound()
        }
    }

    /**
     * Handles incoming call state - plays ringtone and vibrates.
     */
    private fun handleIncomingState(state: RingingState.Incoming) {
        if (!state.acceptedByMe) {
            // Start vibration if allowed
            if (shouldVibrate()) {
                val pattern = streamVideo.vibrationConfig.vibratePattern
                soundPlayer?.vibrate(pattern)
            }

            // Play incoming call sound
            soundPlayer?.playCallSound(
                streamVideo.sounds.ringingConfig.incomingCallSoundUri,
                streamVideo.sounds.mutedRingingConfig?.playIncomingSoundIfMuted ?: false,
            )
        } else {
            // Call accepted - stop sounds immediately for better responsiveness
            soundPlayer?.stopCallSound()
        }
    }

    /**
     * Handles outgoing call state - plays outgoing ringtone.
     */
    private fun handleOutgoingState(state: RingingState.Outgoing) {
        if (!state.acceptedByCallee) {
            soundPlayer?.playCallSound(
                streamVideo.sounds.ringingConfig.outgoingCallSoundUri,
                streamVideo.sounds.mutedRingingConfig?.playOutgoingSoundIfMuted ?: false,
            )
        } else {
            // Call accepted - stop sounds immediately
            soundPlayer?.stopCallSound()
        }
    }

    /**
     * Handles active call state - stops all sounds.
     */
    private fun handleActiveState() {
        soundPlayer?.stopCallSound()
    }

    /**
     * Handles rejected by all state - rejects call and stops service.
     */
    private fun handleRejectedByAllState(onStopService: () -> Unit) {
        ClientScope().launch {
            call.reject(
                source = "RingingState.RejectedByAll",
                reason = RejectReason.Decline,
            )
        }
        soundPlayer?.stopCallSound()
        onStopService()
    }

    /**
     * Handles timeout state - stops sounds.
     */
    private fun handleTimeoutState() {
        soundPlayer?.stopCallSound()
    }

    /**
     * Determines if vibration should be triggered based on ringer mode.
     */
    private fun shouldVibrate(): Boolean {
        if (!streamVideo.vibrationConfig.enabled) return false

        return try {
            val audioManager = streamVideo.context.getSystemService(
                Context.AUDIO_SERVICE,
            ) as AudioManager
            audioManager.ringerMode in listOf(
                AudioManager.RINGER_MODE_NORMAL,
                AudioManager.RINGER_MODE_VIBRATE,
            )
        } catch (e: Exception) {
            logger.e { "Failed to get audio manager: ${e.message}" }
            false
        }
    }
}
