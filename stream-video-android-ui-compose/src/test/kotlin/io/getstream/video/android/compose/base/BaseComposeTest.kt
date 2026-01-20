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

package io.getstream.video.android.compose.base

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import app.cash.paparazzi.Paparazzi
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.avatar.LocalAvatarPreviewPlaceholder
import io.getstream.video.android.mock.StreamPreviewDataUtils

internal abstract class BaseComposeTest {

    abstract fun basePaparazzi(): Paparazzi

    fun snapshot(
        isInDarkMode: Boolean = false,
        composable: @Composable () -> Unit,
    ) {
        basePaparazzi().snapshot {
            StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
            CompositionLocalProvider(
                LocalInspectionMode provides true,
                LocalAvatarPreviewPlaceholder provides
                    io.getstream.video.android.ui.common.R.drawable.stream_video_call_sample,
            ) {
                VideoTheme(isInDarkMode) { composable.invoke() }
            }
        }
    }

    fun snapshotWithDarkMode(composable: @Composable () -> Unit) {
        basePaparazzi().snapshot {
            StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
            CompositionLocalProvider(
                LocalInspectionMode provides true,
                LocalAvatarPreviewPlaceholder provides
                    io.getstream.video.android.ui.common.R.drawable.stream_video_call_sample,
            ) {
                Column {
                    VideoTheme(isInDarkMode = true) { composable.invoke() }
                    VideoTheme(isInDarkMode = false) { composable.invoke() }
                }
            }
        }
    }

    fun snapshotWithDarkModeRow(composable: @Composable () -> Unit) {
        basePaparazzi().snapshot {
            StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
            CompositionLocalProvider(
                LocalInspectionMode provides true,
                LocalAvatarPreviewPlaceholder provides
                    io.getstream.video.android.ui.common.R.drawable.stream_video_call_sample,
            ) {
                Row {
                    VideoTheme(isInDarkMode = true) { composable.invoke() }
                    VideoTheme(isInDarkMode = false) { composable.invoke() }
                }
            }
        }
    }
}
