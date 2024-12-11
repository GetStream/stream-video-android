package io.getstream.video.android.core

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
import org.openapitools.client.models.VideoEvent

private const val TIME_AUTO_REMOVE_CAPTIONS = 8000L //In ms

class CaptionManager {

    private val _closedCaptions: MutableStateFlow<List<CallClosedCaption>> =
        MutableStateFlow(emptyList())
    val closedCaptions: StateFlow<List<CallClosedCaption>> = _closedCaptions.asStateFlow()

    private val _closedCaptionState = MutableStateFlow<ClosedCaptionState>(ClosedCaptionState.UnAvailable)
    val closedCaptionState: StateFlow<ClosedCaptionState> = _closedCaptionState.asStateFlow()

    private var removalJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val mutex = Mutex()

    fun handleCallUpdate(callResponse: CallResponse) {
        val ccMode = callResponse.settings.transcription.closedCaptionMode
        if (callResponse.captioning) {
            _closedCaptionState.value = ClosedCaptionState.Running
        } else {
            if (ccMode == "disabled") {
                _closedCaptionState.value = ClosedCaptionState.UnAvailable
            } else {
                //(ccMode == "auto-on" || ccMode == "available")
                _closedCaptionState.value = ClosedCaptionState.Available
            }

        }
    }

    fun handleEvent(videoEvent: VideoEvent) {
        when (videoEvent) {
            is ClosedCaptionEvent -> addCaption(videoEvent)

            is ClosedCaptionStartedEvent -> {
                _closedCaptionState.value = ClosedCaptionState.Running
            }

            is ClosedCaptionEndedEvent -> {
                _closedCaptionState.value = ClosedCaptionState.Available
            }
        }
    }
    fun addCaption(event: ClosedCaptionEvent) {
        scope.launch {
            mutex.withLock {
                // Add the caption and keep the latest 3
                _closedCaptions.value = (_closedCaptions.value + event.closedCaption).takeLast(3)
            }

            // Cancel and restart the removal job
            removalJob?.cancel()
            scheduleRemoval()
        }
    }

    private fun scheduleRemoval() {
        removalJob = scope.launch {
            delay(TIME_AUTO_REMOVE_CAPTIONS) // Wait for 5 seconds before removing the oldest caption
            mutex.withLock {
                if (_closedCaptions.value.isNotEmpty()) {
                    _closedCaptions.value = _closedCaptions.value.drop(1) // Remove the oldest caption
                }
            }
            if (_closedCaptions.value.isNotEmpty()) {
                scheduleRemoval() // Continue scheduling removal for remaining captions
            }
        }
    }
}

/**
 * sealed class VideoFilter {
 *     data object None : VideoFilter()
 *     data object BlurredBackground : VideoFilter()
 *     data class VirtualBackground(@DrawableRes val drawable: Int) : VideoFilter()
 * }
 */
sealed class ClosedCaptionState {
    data object Available : ClosedCaptionState()
    data object Running : ClosedCaptionState()
    data object UnAvailable : ClosedCaptionState()
}
