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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.theme.design.StreamTokens

/**
 * A modal dialog with a centered title, an optional icon and message, and a stacked action area.
 *
 * The dialog is a platform dialog window, so it moves above the keyboard and dims the content
 * behind it. Actions are usually full width [StreamTextButton]s of size [StreamButtonSize.Large]; the
 * [content] column spaces them by 8dp.
 *
 * @param onDismissRequest Called when the user taps outside the dialog or presses back.
 * @param title The title shown in the heading typography.
 * @param modifier The modifier applied to the dialog card.
 * @param message The supporting text shown below the title, or null for none.
 * @param icon The icon shown above the title, or null for none.
 * @param dismissOnBackPress Whether pressing back calls [onDismissRequest].
 * @param dismissOnClickOutside Whether tapping outside the dialog calls [onDismissRequest].
 * @param content The action area of the dialog, laid out as a column below the texts.
 */
@Composable
public fun StreamDialog(
    onDismissRequest: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    icon: Painter? = null,
    dismissOnBackPress: Boolean = true,
    dismissOnClickOutside: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = dismissOnBackPress,
            dismissOnClickOutside = dismissOnClickOutside,
        ),
    ) {
        Column(
            modifier = modifier
                .widthIn(max = StreamTokens.size320)
                .background(
                    color = VideoTheme.colors.backgroundCoreElevation1,
                    shape = RoundedCornerShape(StreamTokens.radius4xl),
                )
                .padding(
                    start = StreamTokens.spacingXl,
                    top = StreamTokens.spacing2xl,
                    end = StreamTokens.spacingXl,
                    bottom = StreamTokens.spacingXl,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(StreamTokens.spacing2xl),
        ) {
            DialogHeader(title = title, message = message, icon = icon)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(StreamTokens.spacingXs),
                content = content,
            )
        }
    }
}

@Composable
private fun DialogHeader(title: String, message: String?, icon: Painter?) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(StreamTokens.spacingMd),
    ) {
        icon?.let {
            Icon(
                painter = it,
                contentDescription = null,
                modifier = Modifier.size(StreamTokens.iconSizeLg),
                tint = VideoTheme.colors.accentNeutral,
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(StreamTokens.spacingXs),
        ) {
            Text(
                text = title,
                style = VideoTheme.typography.headingSmall,
                color = VideoTheme.colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            message?.let {
                Text(
                    text = it,
                    style = VideoTheme.typography.captionDefault,
                    color = VideoTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
