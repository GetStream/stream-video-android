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

package io.getstream.video.android.core.closedcaptions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.openapitools.client.models.CallClosedCaption
import org.openapitools.client.models.CallResponse
import org.openapitools.client.models.ClosedCaptionEndedEvent
import org.openapitools.client.models.ClosedCaptionEvent
import org.openapitools.client.models.ClosedCaptionStartedEvent
import org.openapitools.client.models.TranscriptionSettingsResponse.ClosedCaptionMode
import org.openapitools.client.models.VideoEvent

/**
 * Manages the lifecycle, state, and configuration of closed captions for a video call.
 *
 * The [ClosedCaptionManager] is responsible for handling caption updates, maintaining caption states,
 * and auto-removing captions based on the provided [ClosedCaptionsSettings]. It ensures thread-safe
 * operations using a [Mutex] and manages jobs for scheduled caption removal using [CoroutineScope].
 *
 * @property closedCaptionsSettings The configuration that defines how closed captions are managed,
 * including auto-dismiss behavior, maximum number of captions to retain, and dismiss time.
 */

class ClosedCaptionManager(private var closedCaptionsSettings: ClosedCaptionsSettings = ClosedCaptionsSettings()) {

    /**
     * Holds the current list of closed captions. This list is updated dynamically
     * and contains at most [ClosedCaptionsSettings.maxVisibleCaptions] captions.
     */

    private val _closedCaptions: MutableStateFlow<List<CallClosedCaption>> =
        MutableStateFlow(emptyList())
    val closedCaptions: StateFlow<List<CallClosedCaption>> = _closedCaptions.asStateFlow()

    /**
     *  Holds the current closed caption mode for the video call. This object contains information about closed
     *  captioning feature availability. This state is updated dynamically based on the server's transcription
     *  setting which is [org.openapitools.client.models.TranscriptionSettingsResponse.closedCaptionMode]
     *
     *  Possible values:
     *  - [ClosedCaptionMode.Available]: Closed captions are available and can be enabled.
     *  - [ClosedCaptionMode.Disabled]: Closed captions are explicitly disabled.
     *  - [ClosedCaptionMode.AutoOn]: Closed captions are automatically enabled as soon as user joins the call
     *  - [ClosedCaptionMode.Unknown]: Represents an unrecognized or unsupported mode.
     */
    private val _ccMode =
        MutableStateFlow<ClosedCaptionMode>(ClosedCaptionMode.Disabled)
    val ccMode: StateFlow<ClosedCaptionMode> = _ccMode.asStateFlow()

    /**
     * Tracks whether closed captioning is currently active for the call.
     * True if captioning is ongoing, false otherwise.
     */
    private val _closedCaptioning: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val closedCaptioning: StateFlow<Boolean> = _closedCaptioning

    /**
     * Manages the job responsible for automatically removing closed captions after a delay.
     */
    private var removalJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /**
     * Ensures thread-safe updates to the list of closed captions.
     */
    private val mutex = Mutex()

    /**
     * Updates the current configuration for the closed captions manager.
     *
     * @param closedCaptionsSettings The new configuration to apply. This affects behavior such as auto-dismiss
     * and the number of captions retained.
     */
    fun updateClosedCaptionsSettings(closedCaptionsSettings: ClosedCaptionsSettings) {
        this.closedCaptionsSettings = closedCaptionsSettings
    }

    /**
     * Handles updates from the call response to determine the availability and state
     * of closed captions.
     *
     * @param callResponse The response containing transcription and caption settings for the call.
     */
    fun handleCallUpdate(callResponse: CallResponse) {
        _closedCaptioning.value = callResponse.captioning
        _ccMode.value = callResponse.settings.transcription.closedCaptionMode
    }

    /**
     * Processes incoming events related to closed captions, such as new captions being added,
     * captioning starting, or captioning ending.
     *
     * @param videoEvent The event containing closed captioning information.
     */
    fun handleEvent(videoEvent: VideoEvent) {
        when (videoEvent) {
            is ClosedCaptionEvent -> {
                addCaption(videoEvent)
                _closedCaptioning.value = true
            }

            is ClosedCaptionStartedEvent -> {
                _closedCaptioning.value = true
            }

            is ClosedCaptionEndedEvent -> {
                _closedCaptioning.value = false
            }
        }
    }

    /**
     * Adds a new caption to the list and manages the auto-dismiss logic.
     *
     * @param event The event containing the closed caption data to add.
     */
    private fun addCaption(event: ClosedCaptionEvent) {
        scope.launch {
            mutex.withLock {
                // Add the caption and keep the latest 3
                _closedCaptions.value =
                    (_closedCaptions.value + event.closedCaption).takeLast(closedCaptionsSettings.maxVisibleCaptions)
            }

            if (closedCaptionsSettings.autoDismissCaptions) {
                removalJob?.cancel()
                scheduleRemoval()
            }
        }
    }

    /**
     * Schedules the removal of the oldest caption after the specified [ClosedCaptionsSettings.visibilityDurationMs].
     *
     */
    private fun scheduleRemoval() {
        removalJob = scope.launch {
            delay(closedCaptionsSettings.visibilityDurationMs)
            mutex.withLock {
                if (_closedCaptions.value.isNotEmpty()) {
                    _closedCaptions.value =
                        _closedCaptions.value.drop(1) // Remove the oldest caption
                }
            }
            if (_closedCaptions.value.isNotEmpty()) {
                scheduleRemoval() // Continue scheduling removal for remaining captions
            }
        }
    }
}
