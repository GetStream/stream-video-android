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

package io.getstream.video.android.ui.xml.widget.control

import io.getstream.video.android.call.state.CallAction

/**
 * A singe call control item to be exposed to the user in the [CallControlsView].
 *
 * @param icon The icon in the center of the button.
 * @param iconTint The color of the icon.
 * @param backgroundTint The color of the button background.
 * @param action The [CallAction] to be performed.
 */
public data class CallControlItem(
    val icon: Int,
    val iconTint: Int,
    val backgroundTint: Int,
    val action: CallAction
)
