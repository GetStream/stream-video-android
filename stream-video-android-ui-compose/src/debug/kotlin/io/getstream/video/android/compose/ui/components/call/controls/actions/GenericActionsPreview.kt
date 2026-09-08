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

package io.getstream.video.android.compose.ui.components.call.controls.actions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import io.getstream.video.android.compose.R
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.base.StreamButtonSize
import io.getstream.video.android.compose.ui.components.base.StreamButtonStyleDefaults

@Preview
@Composable
private fun ToggleActionInProgressRootPreview() {
    VideoTheme {
        ToggleActionInProgressPreview()
    }
}

/**
 * A pending toggle in both states next to a plain action, one row per button size.
 */
@Composable
internal fun ToggleActionInProgressPreview() {
    val icon = painterResource(R.drawable.stream_design_ic_caption_fill)
    Column {
        StreamButtonSize.entries.forEach { size ->
            Row {
                ToggleAction(
                    isActionActive = true,
                    iconOnOff = Pair(icon, icon),
                    progress = true,
                    size = size,
                ) {}
                ToggleAction(
                    isActionActive = false,
                    iconOnOff = Pair(icon, icon),
                    progress = true,
                    size = size,
                ) {}
                GenericAction(icon = icon, size = size) {}
            }
        }
    }
}

@Preview
@Composable
private fun ToggleActionUnavailableRootPreview() {
    VideoTheme {
        ToggleActionUnavailablePreview()
    }
}

/**
 * Unavailable toggles with every parameter set, in both states and in a right-to-left layout so
 * the error badge lands on the other corner.
 */
@Composable
internal fun ToggleActionUnavailablePreview() {
    val icon = painterResource(R.drawable.stream_design_ic_caption_fill)
    Row {
        ToggleAction(
            isActionActive = true,
            iconOnOff = Pair(icon, icon),
            contentDescription = "Captions",
            enabled = true,
            isUnavailable = true,
            onStyle = StreamButtonStyleDefaults.primarySolid,
            offStyle = StreamButtonStyleDefaults.secondaryOutline,
            size = StreamButtonSize.Large,
        ) {}
        ToggleAction(
            isActionActive = false,
            iconOnOff = Pair(icon, icon),
            enabled = false,
            isUnavailable = true,
        ) {}
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            ToggleAction(
                isActionActive = false,
                iconOnOff = Pair(icon, icon),
                isUnavailable = true,
            ) {}
        }
    }
}
