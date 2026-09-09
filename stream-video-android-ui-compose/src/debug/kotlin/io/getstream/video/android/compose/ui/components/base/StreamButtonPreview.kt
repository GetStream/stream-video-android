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

package io.getstream.video.android.compose.ui.components.base

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import io.getstream.video.android.compose.R
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.theme.design.StreamTokens

@Preview
@Composable
private fun StreamButtonStylesEnabledRootPreview() {
    VideoTheme {
        StreamButtonStylesPreview(enabled = true)
    }
}

@Preview
@Composable
private fun StreamButtonStylesDisabledRootPreview() {
    VideoTheme {
        StreamButtonStylesPreview(enabled = false)
    }
}

/**
 * Every [StreamButtonStyleDefaults] variant as an icon button and as a text button with both icons.
 */
@Composable
internal fun StreamButtonStylesPreview(enabled: Boolean) {
    val styles = listOf(
        StreamButtonStyleDefaults.primarySolid,
        StreamButtonStyleDefaults.primaryOutline,
        StreamButtonStyleDefaults.primaryGhost,
        StreamButtonStyleDefaults.secondarySolid,
        StreamButtonStyleDefaults.secondaryOutline,
        StreamButtonStyleDefaults.secondaryGhost,
        StreamButtonStyleDefaults.destructiveSolid,
        StreamButtonStyleDefaults.destructiveOutline,
        StreamButtonStyleDefaults.destructiveGhost,
    )
    val icon = painterResource(R.drawable.stream_design_ic_checkmark)
    Column(
        modifier = Modifier.padding(StreamTokens.spacingMd),
        verticalArrangement = Arrangement.spacedBy(StreamTokens.spacingXs),
    ) {
        styles.forEach { style ->
            Row(horizontalArrangement = Arrangement.spacedBy(StreamTokens.spacingXs)) {
                StreamIconButton(
                    onClick = {},
                    icon = icon,
                    contentDescription = null,
                    enabled = enabled,
                    style = style,
                )
                StreamTextButton(
                    onClick = {},
                    text = "Label",
                    enabled = enabled,
                    style = style,
                    leadingIcon = icon,
                    trailingIcon = icon,
                )
            }
        }
    }
}

@Preview
@Composable
private fun StreamButtonSizesRootPreview() {
    VideoTheme {
        StreamButtonSizesPreview()
    }
}

/**
 * Every [StreamButtonSize] as an icon button and as a text button.
 */
@Composable
internal fun StreamButtonSizesPreview() {
    val icon = painterResource(R.drawable.stream_design_ic_voice_fill)
    Column(
        modifier = Modifier.padding(StreamTokens.spacingMd),
        verticalArrangement = Arrangement.spacedBy(StreamTokens.spacingXs),
    ) {
        StreamButtonSize.entries.forEach { size ->
            Row(horizontalArrangement = Arrangement.spacedBy(StreamTokens.spacingXs)) {
                StreamIconButton(onClick = {}, icon = icon, contentDescription = null, size = size)
                StreamTextButton(onClick = {}, text = "Button $size", size = size)
                StreamButton(
                    onClick = {},
                    size = size,
                    style = StreamButtonStyleDefaults.secondaryOutline,
                ) {
                    Icon(painter = icon, contentDescription = null)
                }
            }
        }
    }
}
