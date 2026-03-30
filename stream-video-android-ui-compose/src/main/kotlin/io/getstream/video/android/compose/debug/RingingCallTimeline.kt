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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.core.Call
import kotlin.math.abs

private const val ENABLE_DEBUG_UI = true

@Composable
internal fun RtcDebugUi(call: Call) {
    if (ENABLE_DEBUG_UI) {
        RingingCallTimeline(call)
    }
}

@Composable
private fun RingingCallTimeline(call: Call) {
    val isCaller by call.state.debugIsCaller.collectAsStateWithLifecycle()

    Column {
        if (isCaller) {
            CallerUi(
                call,
                createCallerTimelineState(call.state.scope, call.state.eventTracker),
                Modifier.alpha(0.7f),
            )
        } else {
            CalleeUi(
                call,
                createCalleeTimelineState(call.state.scope, call.state.eventTracker),
                Modifier.alpha(0.7f),
            )
        }
    }
}

// ── Derived helpers ───────────────────────────────────────────────────────────

private fun List<TimelineEvent>.barScales(): Map<DurationAnchor, Long> =
    groupBy { it.anchor }
        .mapValues { (_, events) -> events.maxOf { abs(it.durationMs) } }

// Returns events sorted by sortMs, tagged with whether the anchor changed
// from the previous event (used to insert dividers).
private data class TaggedEvent(
    val event: TimelineEvent,
    val anchorChanged: Boolean, // true = insert divider above this row
)

private fun List<TimelineEvent>.taggedForDisplay(): List<TaggedEvent> =
    sortedBy { it.sortMs }
        .mapIndexed { i, event ->
            val prev = if (i == 0) null else this.sortedBy { it.sortMs }[i - 1]
            TaggedEvent(
                event = event,
                anchorChanged = prev != null && prev.anchor != event.anchor,
            )
        }

// Slowest event per anchor — used for highlight
private fun List<TimelineEvent>.slowestPerAnchor(): Map<DurationAnchor, TimelineEvent> =
    groupBy { it.anchor }
        .mapValues { (_, events) -> events.maxBy { abs(it.durationMs) } }

// ── Theme tokens ──────────────────────────────────────────────────────────────

private val LabelMuted = Color(0xFF888780)
private val LabelHint = Color(0xFFB4B2A9)
private val DividerColor = Color(0xFFD3D1C7)
private val HighlightRed = Color(0xFFE24B4A)
private val BarTrack = Color(0xFFD3D1C7)

// ── Root composable ───────────────────────────────────────────────────────────

@Composable
private fun CallTimeline(
    heading: String,
    sessionTime: String,
    events: List<UiEvent>,
    modifier: Modifier = Modifier,
) {
    val sorted = events.sortedBy { it.timeStamp }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Text(
            text = heading,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = LabelMuted,
            maxLines = 1,
        )
        sorted.forEach { item ->
            EventRow(event = item)
        }
    }
}

// ── Event row ─────────────────────────────────────────────────────────────────

@Composable
private fun EventRow(
    event: UiEvent,
) {
//    val scale    = barScales[event.anchor] ?: abs(event.durationMs)
//    val fraction = abs(event.durationMs).toFloat() / scale

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        // Line 1: name  [bar]  dur + anchor context
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Event name
            Row(Modifier.weight(1f)) {
                Text(
                    text = event.name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = LabelMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                when(event.category.id) {
                    "http" ->{
                        Spacer(Modifier.width(6.dp))
                        HTTPBadgeUi()
                    }
                    "ui" ->{
                        Spacer(Modifier.width(6.dp))
                        UiBadgeUi()
                    }
                }
            }

            Spacer(Modifier.width(6.dp))
            // Bar
//            DurationBar(
//                fraction = fraction,
//                color = event.category.color,
//                modifier = Modifier.width(44.dp),
//            )
            Spacer(Modifier.width(6.dp))
            // Duration + anchor stacked on the right
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val durStr = buildDurString(event)
                    Text(
                        text = durStr,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = LabelMuted,
                    )
                }
                Text(
                    text = event.durationSince.since,
                    fontSize = 9.sp,
                    color = event.durationSince.color,
                )
            }
        }
        // Line 2: timestamp
        Text(
            text = "at ${event.timestampLabel}",
            fontSize = 10.sp,
            color = LabelHint,
            modifier = Modifier.padding(top = 1.dp),
        )
    }
    ThinDivider()
}

// ── Duration bar ──────────────────────────────────────────────────────────────

@Composable
private fun DurationBar(fraction: Float, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(BarTrack.copy(alpha = 0.5f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .clip(RoundedCornerShape(2.dp))
                .background(color),
        )
    }
}

// ── Shared ────────────────────────────────────────────────────────────────────

@Composable
private fun ThinDivider(
    modifier: Modifier = Modifier,
    color: Color = DividerColor.copy(alpha = 0.5f),
) {
    Box(
        modifier = modifier
            .then(if (modifier == Modifier) Modifier.fillMaxWidth() else modifier)
            .height(0.5.dp)
            .background(color),
    )
}

private fun buildDurString(event: UiEvent): String {
    val abs = "%.1fs".format(abs(event.durationSince.duration) / 1000.0)
    return abs
}

private data class Badge(val label: String, val badgeBg: Color, val badgeFg: Color)
private val HttpBadge = Badge("HTTP", badgeBg = Color(0xFFE6F1FB), badgeFg = Color(0xFF185FA5))
private val UiBadge = Badge("UI", badgeBg = Color(0xFFE6F1FB), badgeFg = Color(0xFF185FA5))

@Composable
private fun HTTPBadgeUi() {
    CategoryBadge(HttpBadge)
}

@Composable
private fun UiBadgeUi() {
    CategoryBadge(UiBadge)
}

@Composable
private fun CategoryBadge(badge: Badge) {
    Text(
        text = badge.label,
        fontSize = 9.sp,
        fontWeight = FontWeight.Medium,
        color = badge.badgeFg,
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(badge.badgeBg)
            .padding(horizontal = 4.dp, vertical = 2.dp),
    )
}

@Composable
private fun CallerUi(
    call: Call,
    state: CallerTimelineState,
    modifier: Modifier = Modifier,
) {
    val callJoined by state.callJoined.collectAsStateWithLifecycle()
    val subscriber by state.subscriber.collectAsStateWithLifecycle()
    val publisher by state.publisher.collectAsStateWithLifecycle()
    val firstInboundRtp by state.firstInboundRtp.collectAsStateWithLifecycle()
    val ringingStateTimerStarted by state.ringingStateTransitionTimerStarted.collectAsStateWithLifecycle()
    val ringingStateTimerFinished by state.ringingStateTransitionTimerFinished.collectAsStateWithLifecycle()

    val visibleEvents by remember {
        derivedStateOf {
            listOfNotNull(
                callJoined,
                subscriber,
                publisher,
                firstInboundRtp,
                ringingStateTimerStarted,
                ringingStateTimerFinished,
            ).sortedBy { it.timeStamp }
        }
    }
    CallTimeline("Caller", "", visibleEvents, Modifier)
}

@Composable
private fun CalleeUi(
    call: Call,
    state: CalleeTimelineState,
    modifier: Modifier = Modifier,
) {
    val callAccepted by state.callAccepted.collectAsStateWithLifecycle()
    val callJoined by state.callJoined.collectAsStateWithLifecycle()
    val subscriber by state.subscriber.collectAsStateWithLifecycle()
    val publisher by state.publisher.collectAsStateWithLifecycle()
    val firstInboundRtp by state.firstInboundRtp.collectAsStateWithLifecycle()
    val ringingStateTimerStarted by state.ringingStateTransitionTimerStarted.collectAsStateWithLifecycle()
    val ringingStateTimerFinished by state.ringingStateTransitionTimerFinished.collectAsStateWithLifecycle()

    val visibleEvents by remember {
        derivedStateOf {
            listOfNotNull(
                callAccepted,
                callJoined,
                subscriber,
                publisher,
                firstInboundRtp,
                ringingStateTimerStarted,
                ringingStateTimerFinished,
            ).sortedBy { it.timeStamp }
        }
    }
    CallTimeline("Callee", "", visibleEvents, Modifier)
}

// ── Preview ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun CallTimelinePreview() {
    val events = listOf(

        UiEvent(
            category = Categories.HTTP,
            name = "Call accepted",
            durationSince = DurationSince(0L, "start", CallStartColor),
            timeStamp = 0L,
            timestampLabel = "10:30:01.0",
        ),

        UiEvent(
            category = Categories.RTP,
            name = "Subscriber connected",
            durationSince = DurationSince(0L, "start", CallStartColor),
            timeStamp = 0L,
            timestampLabel = "10:30:00.6",
        ),
        UiEvent(
            category = Categories.RTP,
            name = "First inbound RTP",
            durationSince = DurationSince(0L, "start", CallStartColor),
            timeStamp = 0L,
            timestampLabel = "10:30:00.9",
        ),
        UiEvent(
            category = Categories.RTP,
            name = "Publisher connected",
            durationSince = DurationSince(0L, "start", CallStartColor),
            timeStamp = 0L,
            timestampLabel = "10:30:01.3",
        ),
        UiEvent(
            category = Categories.HTTP,
            name = "Call accepted",
            durationSince = DurationSince(0L, "start", CallStartColor),
            timeStamp = 0L,
            timestampLabel = "10:30:00.3",
        ),
    )

    VideoTheme {
        CallTimeline(
            heading = "Caller",
            sessionTime = "10:30:00",
            events = events,
        )
    }
}
