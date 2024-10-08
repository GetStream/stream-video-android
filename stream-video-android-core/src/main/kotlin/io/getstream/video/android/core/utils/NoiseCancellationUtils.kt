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

package io.getstream.video.android.core.utils

import org.openapitools.client.models.NoiseCancellationSettings

/**
 * Returns true if the noise cancellation mode is "auto-on".
 */
val NoiseCancellationSettings.isAutoOn get() = mode == NoiseCancellationSettings.Mode.AutoOn

/**
 * Returns true if the noise cancellation mode is "available".
 */
val NoiseCancellationSettings.isAvailable get() = mode == NoiseCancellationSettings.Mode.Available

/**
 * Returns true if the noise cancellation mode is "disabled".
 */
val NoiseCancellationSettings.isDisabled get() = mode == NoiseCancellationSettings.Mode.Disabled

/**
 * Returns true if the noise cancellation mode is "auto-on" or "available".
 */
val NoiseCancellationSettings.isEnabled get() = when (mode) {
    NoiseCancellationSettings.Mode.Available,
    NoiseCancellationSettings.Mode.AutoOn,
    -> true
    else -> false
}
