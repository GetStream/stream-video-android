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

package io.getstream.video.android.compose.ui

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.android.resources.ScreenOrientation
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.avatar.LocalAvatarPreviewPlaceholder
import io.getstream.video.android.mock.StreamPreviewDataUtils
import org.junit.Rule

/**
 * [DeviceConfig.PIXEL_2] geometry (411x731dp) at hdpi. The dp layout is identical to the
 * Pixel 2, but rendering at 1.5x keeps the golden files small, which speeds up comparisons.
 */
internal val PIXEL_2_HDPI = DeviceConfig.PIXEL_2.atHdpi()

/**
 * [DeviceConfig.PIXEL_4A] geometry (392x850dp) at hdpi, for snapshots that need a taller screen.
 */
internal val PIXEL_4A_HDPI = DeviceConfig.PIXEL_4A.atHdpi()

/**
 * [DeviceConfig.PIXEL_2] landscape geometry (731x411dp) at hdpi, for landscape snapshots.
 */
internal val PIXEL_2_LANDSCAPE_HDPI = PIXEL_2_HDPI.copy(
    orientation = ScreenOrientation.LANDSCAPE,
)

/**
 * A copy of this device with the same dp geometry rendered at 1.5x instead of the native density.
 */
private fun DeviceConfig.atHdpi(): DeviceConfig = copy(
    screenWidth = screenWidth * Density.HIGH.dpiValue / density.dpiValue,
    screenHeight = screenHeight * Density.HIGH.dpiValue / density.dpiValue,
    xdpi = Density.HIGH.dpiValue,
    ydpi = Density.HIGH.dpiValue,
    density = Density.HIGH,
)

internal interface PaparazziComposeTest {

    @get:Rule
    val paparazzi: Paparazzi

    fun snapshot(
        isInDarkMode: Boolean = false,
        contentAlignment: Alignment = Alignment.TopStart,
        backgroundColor: Color = Color.Unspecified,
        composable: @Composable () -> Unit,
    ) {
        paparazzi.snapshot {
            TestEnvironment {
                VideoTheme(isInDarkMode = isInDarkMode) {
                    Box(
                        modifier = Modifier
                            .background(
                                backgroundColor.takeOrElse { VideoTheme.colors.baseSheetPrimary },
                            ),
                        contentAlignment = contentAlignment,
                    ) {
                        composable()
                    }
                }
            }
        }
    }

    fun snapshotWithDarkMode(
        contentAlignment: Alignment = Alignment.TopStart,
        composable: @Composable () -> Unit,
    ) {
        paparazzi.snapshot {
            TestEnvironment {
                Column {
                    VideoTheme(isInDarkMode = true) {
                        Box(
                            modifier = Modifier
                                .weight(weight = .5f, fill = false)
                                .background(VideoTheme.colors.baseSheetPrimary),
                            contentAlignment = contentAlignment,
                        ) {
                            composable()
                        }
                    }
                    VideoTheme(isInDarkMode = false) {
                        Box(
                            modifier = Modifier
                                .weight(weight = .5f, fill = false)
                                .background(VideoTheme.colors.baseSheetPrimary),
                            contentAlignment = contentAlignment,
                        ) {
                            composable()
                        }
                    }
                }
            }
        }
    }

    fun snapshotWithDarkModeRow(
        contentAlignment: Alignment = Alignment.TopStart,
        composable: @Composable () -> Unit,
    ) {
        paparazzi.snapshot {
            TestEnvironment {
                Row {
                    VideoTheme(isInDarkMode = true) {
                        Box(
                            modifier = Modifier
                                .weight(weight = .5f, fill = false)
                                .background(VideoTheme.colors.baseSheetPrimary),
                            contentAlignment = contentAlignment,
                        ) {
                            composable()
                        }
                    }
                    VideoTheme(isInDarkMode = false) {
                        Box(
                            modifier = Modifier
                                .weight(weight = .5f, fill = false)
                                .background(VideoTheme.colors.baseSheetPrimary),
                            contentAlignment = contentAlignment,
                        ) {
                            composable()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TestEnvironment(content: @Composable () -> Unit) {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    CompositionLocalProvider(
        LocalInspectionMode provides true,
        LocalOnBackPressedDispatcherOwner provides FakeBackDispatcherOwner,
        LocalAvatarPreviewPlaceholder provides
            io.getstream.video.android.ui.common.R.drawable.stream_video_call_sample,
        content = content,
    )
}

/**
 * A fake [OnBackPressedDispatcherOwner] necessary for composable components that use [BackHandler].
 */
private val FakeBackDispatcherOwner = object : OnBackPressedDispatcherOwner {
    private val dispatcher = OnBackPressedDispatcher()

    override val onBackPressedDispatcher: OnBackPressedDispatcher = dispatcher

    override val lifecycle: Lifecycle = LifecycleRegistry.createUnsafe(this).apply {
        currentState = Lifecycle.State.RESUMED
    }
}
