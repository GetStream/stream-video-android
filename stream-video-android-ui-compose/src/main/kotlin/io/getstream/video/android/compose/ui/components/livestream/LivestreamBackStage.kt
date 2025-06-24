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

package io.getstream.video.android.compose.ui.components.livestream

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.core.Call
import io.getstream.video.android.ui.common.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Composable
internal fun BoxScope.LivestreamBackStage(call: Call) {
    Column(
        modifier = Modifier.align(Alignment.Center),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(
                id = R.string.stream_video_livestreaming_on_backstage,
            ),
            fontSize = 18.sp,
            color = VideoTheme.colors.basePrimary,
        )
        Spacer(Modifier.height(16.dp))

        val startsAt = call.state.startsAt.collectAsStateWithLifecycle()

        val targetUtcTime = Clock.System.now()
            .plus(10.seconds)
            .toString()

        CountDownTimerUi(targetUtcTime)
        ParticipantCountUi(call)
    }
}

@Composable
internal fun ParticipantCountUi(call: Call) {
    val waitingCount by call.state.session.map {
        it?.participants?.count { it.role != "host" }
    }.collectAsStateWithLifecycle(null)

    waitingCount?.let {
        Spacer(Modifier.height(16.dp))
        Text(
            "$it participants have joined early",
            fontSize = 16.sp,
            color = VideoTheme.colors.baseSecondary,
        )
    }
}

@SuppressLint("DefaultLocale")
@Composable
internal fun CountDownTimerUi(targetUtcTime: String) {
    val targetTime = remember(targetUtcTime) {
        // Parse the target UTC time string (e.g., "2025-06-20T10:00:00Z")
        Instant.parse(targetUtcTime)
    }

    var timeLeft by remember { mutableStateOf(Duration.ZERO) }

    LaunchedEffect(targetTime) {
        while (true) {
            val now = Clock.System.now()
            timeLeft = targetTime - now
            delay(1000L)
        }
    }

    var countDownText = ""
    if (timeLeft.isNegative()) {
        countDownText = stringResource(R.string.stream_video_livestreaming_countdown_finished)
    } else {
        val hours = timeLeft.inWholeHours
        val minutes = (timeLeft.inWholeMinutes % 60)
        val seconds = (timeLeft.inWholeSeconds % 60)

        countDownText = String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    Text(
        modifier = Modifier,
        text = countDownText,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = VideoTheme.colors.basePrimary,
    )
}

@Preview(
    name = "Portrait Preview",
    showBackground = true,
    uiMode = Configuration.UI_MODE_TYPE_NORMAL,
    device = "spec:width=411dp,height=891dp,dpi=420",
)
@Composable
private fun LivestreamBackstagePortraitPreview() {
    VideoTheme {
        Box {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(
                        id = R.string.stream_video_livestreaming_on_backstage,
                    ),
                    fontSize = 14.sp,
                    color = VideoTheme.colors.basePrimary,
                )
                Text(
                    modifier = Modifier,
                    text = "2:00",
                    fontSize = 16.sp,
                    color = VideoTheme.colors.basePrimary,
                )
                Text(
                    modifier = Modifier,
                    text = "2 participants have joined the call",
                    fontSize = 12.sp,
                    color = VideoTheme.colors.baseSecondary,
                )
            }
        }
    }
}
