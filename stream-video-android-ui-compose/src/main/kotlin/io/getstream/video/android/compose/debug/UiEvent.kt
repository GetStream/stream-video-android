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

package io.getstream.video.android.compose.debug

import androidx.compose.ui.graphics.Color
import io.getstream.video.android.core.call.connection.trackers.EventTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

internal data class CalleeTimelineState(
    val calleeName: String,
    val callAccepted: StateFlow<UiEvent?>,
    val callJoined: StateFlow<UiEvent?>,
    val subscriber: StateFlow<UiEvent?>,
    val publisher: StateFlow<UiEvent?>,
    val firstInboundRtp: StateFlow<UiEvent?>,
    val ringingStateTransitionTimerStarted: StateFlow<UiEvent?>,
    val ringingStateTransitionTimerFinished: StateFlow<UiEvent?>,
)

internal data class CallerTimelineState(
    val calleeName: String,
    val callJoined: StateFlow<UiEvent?>,
    val subscriber: StateFlow<UiEvent?>,
    val publisher: StateFlow<UiEvent?>,
    val firstInboundRtp: StateFlow<UiEvent?>,
    val ringingStateTransitionTimerStarted: StateFlow<UiEvent?>,
    val ringingStateTransitionTimerFinished: StateFlow<UiEvent?>,
)

internal fun createCalleeTimelineState(scope: CoroutineScope, eventTracker: EventTracker): CalleeTimelineState {
    val callAccepted = combine(
        eventTracker.acceptedStartEvent,
        eventTracker.acceptedEndEvent,
    ) { start, end ->
        if (start.time == null || end.time == null) return@combine null
        UiEvent(
            name = "Call accepted",
            category = Categories.HTTP,
            durationSince = DurationSince(
                end.time!! - start.time!!,
                "since call start",
                CallStartColor,
            ),
            timeStamp = start.time!!,
            timestampLabel = formatTimestamp(start.time!!),
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = null,
    )

    val callJoined = combine(eventTracker.joinStartEvent, eventTracker.joinEndEvent) { start, end ->
        if (start.time == null || end.time == null) return@combine null
        UiEvent(
            name = "Call Joined",
            category = Categories.HTTP,
            durationSince = DurationSince(
                end.time!! - start.time!!,
                "since join start",
                JoinStartColor,
            ),
            timeStamp = start.time!!,
            timestampLabel = formatTimestamp(start.time!!),
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = null,
    )

    val subscriberConnected = combine(
        eventTracker.subscriberConnectedEvent,
        eventTracker.joinEndEvent,
    ) { start, end ->
        if (start.time == null || end.time == null) return@combine null
        UiEvent(
            name = "Subscriber Connected",
            category = Categories.RTP,
            durationSince = DurationSince(
                start.time!! - end.time!!,
                "since join finish",
                JoinFinishColor,
            ),
            timeStamp = start.time!!,
            timestampLabel = formatTimestamp(start.time!!),
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = null,
    )

    val publisherConnected = combine(
        eventTracker.publisherConnectedEvent,
        eventTracker.joinEndEvent,
    ) { start, end ->
        if (start.time == null || end.time == null) return@combine null
        UiEvent(
            name = "Publisher Connected",
            category = Categories.RTP,
            durationSince = DurationSince(
                start.time!! - end.time!!,
                "since join finish",
                JoinFinishColor,
            ),
            timeStamp = start.time!!,
            timestampLabel = formatTimestamp(start.time!!),
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = null,
    )

    val firstInboundRtpReceived = combine(
        eventTracker.firstInboundRtpArrived,
        eventTracker.joinEndEvent,
    ) { start, end ->
        if (start.time == null || end.time == null) return@combine null
        UiEvent(
            name = "First RTP Arrived",
            category = Categories.RTP,
            durationSince = DurationSince(
                start.time!! - end.time!!,
                "since join finish",
                JoinFinishColor,
            ),
            timeStamp = start.time!!,
            timestampLabel = formatTimestamp(start.time!!),
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = null,
    )

    val ringingStateTimerStarted = combine(
        eventTracker.ringingStateTimerStarted,
        eventTracker.joinEndEvent,
    ) { start, end ->
        if (start.time == null || end.time == null) return@combine null
        UiEvent(
            name = "Ringing Timer Started",
            category = Categories.UI,
            durationSince = DurationSince(
                start.time!! - end.time!!,
                "since join finish",
                JoinFinishColor,
            ),
            timeStamp = start.time!!,
            timestampLabel = formatTimestamp(start.time!!),
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = null,
    )

    val ringingStateTimerFinished = combine(
        eventTracker.ringingStateTimerFinished,
        eventTracker.joinEndEvent,
    ) { start, end ->
        if (start.time == null || end.time == null) return@combine null
        UiEvent(
            name = "Ringing Timer Finished",
            category = Categories.UI,
            durationSince = DurationSince(
                start.time!! - end.time!!,
                "since join finish",
                JoinFinishColor,
            ),
            timeStamp = start.time!!,
            timestampLabel = formatTimestamp(start.time!!),
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = null,
    )

    return CalleeTimelineState(
        "callee",
        callAccepted,
        callJoined,
        subscriberConnected,
        publisherConnected,
        firstInboundRtpReceived,
        ringingStateTimerStarted,
        ringingStateTimerFinished,
    )
}

internal fun createCallerTimelineState(scope: CoroutineScope, eventTracker: EventTracker): CallerTimelineState {
    val callJoined = combine(eventTracker.joinStartEvent, eventTracker.joinEndEvent) { start, end ->
        if (start.time == null || end.time == null) return@combine null
        UiEvent(
            name = "Call Joined",
            category = Categories.HTTP,
            durationSince = DurationSince(
                end.time!! - start.time!!,
                "since join start",
                JoinStartColor,
            ),
            timeStamp = start.time!!,
            timestampLabel = formatTimestamp(start.time!!),
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = null,
    )

    val subscriberConnected = combine(
        eventTracker.subscriberConnectedEvent,
        eventTracker.joinEndEvent,
    ) { start, end ->
        if (start.time == null || end.time == null) return@combine null
        UiEvent(
            name = "Subscriber Connected",
            category = Categories.RTP,
            durationSince = DurationSince(
                start.time!! - end.time!!,
                "since join finish",
                JoinFinishColor,
            ),
            timeStamp = start.time!!,
            timestampLabel = formatTimestamp(start.time!!),
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = null,
    )

    val publisherConnected = combine(
        eventTracker.publisherConnectedEvent,
        eventTracker.joinEndEvent,
    ) { start, end ->
        if (start.time == null || end.time == null) return@combine null
        UiEvent(
            name = "Publisher Connected",
            category = Categories.RTP,
            durationSince = DurationSince(
                start.time!! - end.time!!,
                "since join finish",
                JoinFinishColor,
            ),
            timeStamp = start.time!!,
            timestampLabel = formatTimestamp(start.time!!),
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = null,
    )

    val firstInboundRtpReceived = combine(
        eventTracker.subscriberConnectedEvent,
        eventTracker.joinEndEvent,
    ) { start, end ->
        if (start.time == null || end.time == null) return@combine null
        UiEvent(
            name = "First RTP Arrived",
            category = Categories.RTP,
            durationSince = DurationSince(
                start.time!! - end.time!!,
                "since join finish",
                JoinFinishColor,
            ),
            timeStamp = start.time!!,
            timestampLabel = formatTimestamp(start.time!!),
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = null,
    )

    val ringingStateTimerStarted = combine(
        eventTracker.ringingStateTimerStarted,
        eventTracker.joinEndEvent,
    ) { start, end ->
        if (start.time == null || end.time == null) return@combine null
        UiEvent(
            name = "Ringing Timer Started",
            category = Categories.UI,
            durationSince = DurationSince(
                start.time!! - end.time!!,
                "since join finish",
                JoinFinishColor,
            ),
            timeStamp = start.time!!,
            timestampLabel = formatTimestamp(start.time!!),
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = null,
    )

    val ringingStateTimerFinished = combine(
        eventTracker.ringingStateTimerFinished,
        eventTracker.joinEndEvent,
    ) { start, end ->
        if (start.time == null || end.time == null) return@combine null
        UiEvent(
            name = "Ringing Timer Finished",
            category = Categories.UI,
            durationSince = DurationSince(
                start.time!! - end.time!!,
                "since join finish",
                JoinFinishColor,
            ),
            timeStamp = start.time!!,
            timestampLabel = formatTimestamp(start.time!!),
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = null,
    )

    return CallerTimelineState(
        "caller",
        callJoined,
        subscriberConnected,
        publisherConnected,
        firstInboundRtpReceived,
        ringingStateTimerStarted,
        ringingStateTimerFinished,
    )
}

internal fun formatTimestamp(epochMs: Long): String {
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = epochMs }
    return "%02d:%02d:%02d.%d".format(
        cal.get(java.util.Calendar.HOUR_OF_DAY),
        cal.get(java.util.Calendar.MINUTE),
        cal.get(java.util.Calendar.SECOND),
        cal.get(java.util.Calendar.MILLISECOND) / 100,
    )
}
internal data class UiEvent(
    val name: String,
    val category: TimelineCategory,
    val durationSince: DurationSince,
    val timeStamp: Long,
    val timestampLabel: String,
)

internal data class DurationSince(val duration: Long, val since: String, val color: Color)
internal val CallStartColor = Color(0xFF7F77DD)
internal val JoinStartColor = Color(0xFF888780)
internal val JoinFinishColor = Color(0xFF1D9E75)
internal object Categories {
    val HTTP = TimelineCategory(
        id = "http",
        label = "HTTP",
        color = Color(0xFF888780),
    )
    val RTP = TimelineCategory(
        id = "rtp",
        label = "First inbound RTP",
        color = Color(0xFF888780),
    )

    val UI = TimelineCategory(
        id = "ui",
        label = "UI",
        color = Color(0xFF888780),
    )
}

internal enum class DurationAnchor(val shortLabel: String, val color: Color) {
    CALL_START(shortLabel = "call start", color = Color(0xFF7F77DD)),
    JOIN_FINISH(shortLabel = "join finish", color = Color(0xFF1D9E75)),
}

// ── Visual identity ───────────────────────────────────────────────────────────

internal data class TimelineCategory(
    val id: String,
    val label: String,
    val color: Color,
)

// ── Single model ──────────────────────────────────────────────────────────────

internal data class TimelineEvent(
    val category: TimelineCategory,
    val name: String,
    val anchor: DurationAnchor,
    val durationMs: Long, // signed — negative = before anchor
    val sortMs: Long, // absolute ms from call start
    val timestampLabel: String,
)
