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

package io.getstream.video.android.ui.moderation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

// @Composable
// internal fun ModerationWarningUiContainer(
//    call: Call,
//    config: ModerationThemeConfig = ModerationDefaults.defaultTheme,
//    moderationWarningAnimationConfig: ModerationWarningAnimationConfig =
//        ModerationWarningAnimationConfig(),
//    moderationText: ModerationText,
// ) {
//    if (LocalInspectionMode.current) {
//        Box(
//            modifier = Modifier
//                .fillMaxSize()
//                .offset(y = config.yOffset)
//                .padding(horizontal = config.horizontalMargin),
//
//            contentAlignment = Alignment.BottomCenter,
//        ) {
//            ModerationWarningUiContentDemo()
//        }
//    } else {
//        Box(
//            modifier = Modifier
//                .fillMaxSize()
//                .offset(y = config.yOffset)
//                .padding(horizontal = config.horizontalMargin),
//            contentAlignment = Alignment.BottomCenter,
//        ) {
//            ModerationUi(config, moderationWarningAnimationConfig, moderationText)
//        }
//    }
// }
//
// @Composable
// internal fun ModerationUi(
//    moderationThemeConfig: ModerationThemeConfig,
//    moderationWarningAnimationConfig: ModerationWarningAnimationConfig,
//    moderationText: ModerationText,
// ) {
//    SlideInOutMessage(moderationThemeConfig, moderationWarningAnimationConfig, moderationText)
// }
//
// @Composable
// private fun SlideInOutMessage(
//    moderationThemeConfig: ModerationThemeConfig,
//    moderationWarningAnimationConfig: ModerationWarningAnimationConfig,
//    moderationText: ModerationText,
// ) {
//    var visible by remember { mutableStateOf(false) }
//
//    // Trigger visibility when composable enters composition
//    LaunchedEffect(Unit) {
//        visible = true
//        delay(moderationWarningAnimationConfig.displayTime) // visible for 3 seconds
//        visible = false
//    }
//
//    Box(
//        modifier = Modifier.fillMaxSize(),
//        contentAlignment = Alignment.BottomCenter,
//    ) {
//        AnimatedVisibility(
//            visible = visible,
//            enter = slideInVertically(
//                initialOffsetY = { fullHeight -> fullHeight },
//                animationSpec = tween(durationMillis = 500, easing = EaseOutCubic),
//            ) + fadeIn(
//                animationSpec = tween(durationMillis = 500),
//            ),
//            exit = slideOutVertically(
//                targetOffsetY = { fullHeight -> fullHeight },
//                animationSpec = tween(durationMillis = 500, easing = EaseInCubic),
//            ) + fadeOut(
//                animationSpec = tween(durationMillis = 500),
//            ),
//        ) {
//            ModerationWarningUiContent(moderationThemeConfig, moderationText)
//        }
//    }
// }
//
// @Composable
// internal fun ModerationWarningUiContent(
//    moderationThemeConfig: ModerationThemeConfig,
//    moderationText: ModerationText,
// ) {
//    Box(
//        modifier = Modifier
//            .fillMaxWidth()
//            .background(
//                Color.White,
//                shape = RoundedCornerShape(
//                    topStart = 8.dp,
//                    bottomStart = 8.dp,
//                    topEnd = 8.dp,
//                    bottomEnd = 8.dp,
//                ),
//            ),
//        contentAlignment = Alignment.TopStart,
//    ) {
//        Row(
//            modifier = Modifier.height(IntrinsicSize.Min),
//        ) {
//            // Orange column on the left
//            Box(
//                modifier = Modifier
//                    .width(moderationThemeConfig.warningStripWidth)
//                    .fillMaxHeight()
//                    .background(
//                        moderationThemeConfig.warningStripColor,
//                        shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
//                    ),
//            )
//
//            Column(Modifier.padding(vertical = 12.dp, horizontal = 12.dp)) {
//                Text(
//                    text = moderationText.title,
//                    fontWeight = FontWeight.Bold,
//                    color = moderationThemeConfig.titleColor,
//                    fontSize = 16.sp,
//                )
//                Text(
//                    text = moderationText.message,
//                    color = moderationThemeConfig.messageColor,
//                    fontSize = 16.sp,
//                )
//            }
//        }
//    }
// }
//
// @Preview
// @Composable
// internal fun ModerationWarningUiContentDemo() {
//    ModerationWarningUiContent(
//        ModerationThemeConfig(),
//        ModerationText("Warning title", "Warning Message"),
//    )
// }
