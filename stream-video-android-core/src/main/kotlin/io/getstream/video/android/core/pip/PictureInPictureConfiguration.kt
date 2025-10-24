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

package io.getstream.video.android.core.pip

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Configuration for controlling Picture-in-Picture (PiP) behavior within a call UI.
 *
 * @property enable
 * Indicates whether Picture-in-Picture mode should be enabled for this call.
 * When `true`, the SDK will handle entering PiP mode automatically when appropriate
 * (for example, when the user navigates away from the app during an active call).
 *
 * @property autoEnterEnabled
 * Determines whether to call [android.app.PictureInPictureParams.Builder.setAutoEnterEnabled]
 * when configuring the PiP parameters. When `true`, the system automatically enters PiP mode
 * when the user presses the home button or performs an equivalent action. Set this to `false`
 * if you prefer to manually control when PiP mode should be entered.
 */
@Parcelize
public data class PictureInPictureConfiguration(
    val enable: Boolean,
    val autoEnterEnabled: Boolean = true,
) : Parcelable
