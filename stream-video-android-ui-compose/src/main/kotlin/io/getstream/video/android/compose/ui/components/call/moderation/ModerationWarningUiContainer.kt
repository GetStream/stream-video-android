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

package io.getstream.video.android.compose.ui.components.call.moderation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInCubic
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import io.getstream.video.android.compose.R
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.theme.design.StreamTokens
import io.getstream.video.android.core.Call
import io.getstream.video.android.ui.moderation.ModerationDefaults
import io.getstream.video.android.ui.moderation.ModerationThemeConfig
import kotlinx.coroutines.delay

@Composable
internal fun DefaultModerationWarningUiContainer(
    call: Call,
    message: String? = null,
    config: ModerationThemeConfig = defaultModerationThemeConfig(),
    moderationWarningAnimationConfig: ModerationWarningAnimationConfig =
        ModerationWarningAnimationConfig(),
) {
    if (LocalInspectionMode.current) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = config.yOffset)
                .padding(horizontal = config.horizontalMargin),

            contentAlignment = Alignment.BottomCenter,
        ) {
            ModerationWarningUiContentDemo()
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = config.yOffset)
                .padding(horizontal = config.horizontalMargin),
            contentAlignment = Alignment.BottomCenter,
        ) {
            val defaultTitle = LocalContext.current.getString(
                R.string.stream_default_moderation_warning_title,
            )
            val defaultMessage = message ?: LocalContext.current.getString(R.string.stream_default_moderation_warning_message)
            val moderationText = ModerationText(defaultTitle, defaultMessage)
            ModerationUi(config, moderationWarningAnimationConfig, moderationText)
        }
    }
}

@Composable
internal fun ModerationUi(
    moderationThemeConfig: ModerationThemeConfig,
    moderationWarningAnimationConfig: ModerationWarningAnimationConfig,
    moderationText: ModerationText,
) {
    SlideInOutMessage(moderationThemeConfig, moderationWarningAnimationConfig, moderationText)
}

@Composable
private fun SlideInOutMessage(
    moderationThemeConfig: ModerationThemeConfig,
    moderationWarningAnimationConfig: ModerationWarningAnimationConfig,
    moderationText: ModerationText,
) {
    var visible by remember { mutableStateOf(false) }

    // Trigger visibility when composable enters composition
    LaunchedEffect(Unit) {
        visible = true
        delay(moderationWarningAnimationConfig.displayTime) // visible for 3 seconds
        visible = false
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { fullHeight -> fullHeight },
                animationSpec = tween(durationMillis = 500, easing = EaseOutCubic),
            ) + fadeIn(
                animationSpec = tween(durationMillis = 500),
            ),
            exit = slideOutVertically(
                targetOffsetY = { fullHeight -> fullHeight },
                animationSpec = tween(durationMillis = 500, easing = EaseInCubic),
            ) + fadeOut(
                animationSpec = tween(durationMillis = 500),
            ),
        ) {
            ModerationWarningUiContent(moderationThemeConfig, moderationText)
        }
    }
}

@Composable
internal fun ModerationWarningUiContent(
    moderationThemeConfig: ModerationThemeConfig,
    moderationText: ModerationText,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                moderationThemeConfig.backgroundColor,
                shape = RoundedCornerShape(StreamTokens.radiusMd),
            ),
        contentAlignment = Alignment.TopStart,
    ) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
        ) {
            // Warning strip on the left
            Box(
                modifier = Modifier
                    .width(moderationThemeConfig.warningStripWidth)
                    .fillMaxHeight()
                    .background(
                        moderationThemeConfig.warningStripColor,
                        shape = RoundedCornerShape(
                            topStart = StreamTokens.radiusXl,
                            topEnd = ZeroCornerSize,
                            bottomEnd = ZeroCornerSize,
                            bottomStart = StreamTokens.radiusXl,
                        ),
                    ),
            )

            Column(Modifier.padding(StreamTokens.spacingSm)) {
                Text(
                    text = moderationText.title,
                    color = moderationThemeConfig.titleColor,
                    style = VideoTheme.typography.headingSmall,
                )
                Text(
                    text = moderationText.message,
                    color = moderationThemeConfig.messageColor,
                    style = VideoTheme.typography.bodyDefault,
                )
            }
        }
    }
}

/**
 * The default moderation warning look, taken from the theme colors.
 */
@Composable
internal fun defaultModerationThemeConfig(): ModerationThemeConfig =
    ModerationDefaults.defaultTheme.copy(
        backgroundColor = VideoTheme.colors.backgroundCoreElevation3,
        titleColor = VideoTheme.colors.textPrimary,
        messageColor = VideoTheme.colors.textSecondary,
        warningStripColor = VideoTheme.colors.accentWarning,
    )

@Composable
internal fun ModerationWarningUiContentDemo() {
    ModerationWarningUiContent(
        defaultModerationThemeConfig(),
        ModerationText("Warning title", "Warning Message"),
    )
}
