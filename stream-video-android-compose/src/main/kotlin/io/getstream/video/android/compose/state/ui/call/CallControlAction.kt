/*
 * Copyright (c) 2014-2022 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.compose.state.ui.call

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import io.getstream.video.android.core.call.state.CallAction

/**
 * Represents a single Call Control item in the UI.
 *
 * @param actionBackgroundTint The tint of the background for the action button.
 * @param icon The icon within the button.
 * @param iconTint The tint of the action icon.
 * @param callAction The action that this item represents.
 * @param description The content description of the item.
 */
public data class CallControlAction(
    val actionBackgroundTint: Color,
    val icon: Painter,
    val iconTint: Color,
    val callAction: CallAction,
    val description: String? = null
)
