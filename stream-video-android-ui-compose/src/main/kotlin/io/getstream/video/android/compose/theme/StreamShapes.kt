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

package io.getstream.video.android.compose.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Contains all the shapes we provide for our components.
 *
 * @param avatar Used for avatar UIs in the SDK.
 * @param dialog Used for dialog UIs in the SDK, such as user invites.
 * @param callButton The shape of call buttons.
 * @param callControls The shape of the call controls sheet when in a call.
 * @param callControlsButton Tha shape of the buttons within Call Controls.
 * @param participantsInfoMenuButton The shape of buttons in the Participants Info menu.
 * @param indicatorBackground The indicator background shape.
 */
@Immutable
public data class StreamShapes(
    public val circle: Shape,
    public val square: Shape,
    public val button: Shape,
    public val input: Shape,
    public val dialog: Shape,
    public val sheet: Shape,
    public val indicator: Shape,
    public val container: Shape,
) {
    public companion object {
        /**
         * Builds the default shapes for our theme.
         *
         * @return A [StreamShapes] that holds our default shapes.
         */
        @Composable
        public fun defaultShapes(dimens: StreamDimens): StreamShapes = StreamShapes(
            circle = CircleShape,
            button = RoundedCornerShape(dimens.roundnessXl),
            input = RoundedCornerShape(dimens.roundnessXl),
            sheet = RoundedCornerShape(dimens.roundnessM),
            dialog = RoundedCornerShape(dimens.roundnessL),
            container = RoundedCornerShape(dimens.roundnessXl),
            indicator = RoundedCornerShape(dimens.roundnessS),
            square = CutCornerShape(CornerSize(0.dp)),
        )
    }
}
