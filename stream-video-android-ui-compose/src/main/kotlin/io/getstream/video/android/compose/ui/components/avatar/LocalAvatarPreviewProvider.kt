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

package io.getstream.video.android.compose.ui.components.avatar

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import io.getstream.video.android.ui.common.R

/**
 * Local containing the preferred preview placeholder for providing the same instance
 * in our composable hierarchy.
 */
public val LocalAvatarPreviewPlaceholder: ProvidableCompositionLocal<Int> =
    staticCompositionLocalOf { R.drawable.stream_video_call_sample }

/**
 * Local containing the preferred loading placeholder for providing the same instance
 * in our composable hierarchy.
 */
public val LocalAvatarLoadingPlaceholder: ProvidableCompositionLocal<Int?> =
    staticCompositionLocalOf { R.drawable.stream_video_ic_preview_avatar }

/** A provider for taking the local instances related to the [Avatar] composable. */
internal object LocalAvatarPreviewProvider {

    @Composable
    @DrawableRes
    fun getLocalAvatarPreviewPlaceholder(): Int {
        return LocalAvatarPreviewPlaceholder.current
    }

    @Composable
    @DrawableRes
    fun getLocalAvatarLoadingPlaceholder(): Int? {
        return LocalAvatarLoadingPlaceholder.current
    }
}
