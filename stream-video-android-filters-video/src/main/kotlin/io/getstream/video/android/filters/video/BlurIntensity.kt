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

package io.getstream.video.android.filters.video

import androidx.annotation.Keep

/**
 * The intensity of the background blur effect. Used in [BlurredBackgroundVideoFilter].
 * The class names are in capital letter to maintain backward-compatibility
 */
@Keep
public sealed class BlurIntensity(public val radius: Int) {
    public data object LIGHT : BlurIntensity(7)
    public data object MEDIUM : BlurIntensity(11)
    public data object HEAVY : BlurIntensity(16)
    public data object ULTRA : BlurIntensity(25)
}
