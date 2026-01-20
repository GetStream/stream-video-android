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

@file:OptIn(ExperimentalMaterialApi::class)

package io.getstream.video.android.compose.theme

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalContentColor
import androidx.compose.material.LocalRippleConfiguration
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RippleConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable

/**
 * A modified version of the default [RippleConfiguration] from [MaterialTheme] which
 * works in case the [MaterialTheme] is not initialized.
 */
public object StreamRippleConfiguration {

    @Composable
    @ReadOnlyComposable
    public fun default(): RippleConfiguration {
        val rippleConfiguration = LocalRippleConfiguration.current
        if (rippleConfiguration != null) return rippleConfiguration

        val contentColor = LocalContentColor.current
        return RippleConfiguration(color = contentColor)
    }
}
