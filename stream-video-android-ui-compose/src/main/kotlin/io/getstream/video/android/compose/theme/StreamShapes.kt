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

package io.getstream.video.android.compose.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.RectangleShape
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
    public val avatar: Shape,
    public val dialog: Shape,
    public val callButton: Shape,
    public val callControls: Shape,
    public val callControlsLandscape: Shape,
    public val callControlsButton: Shape,
    public val participantsInfoMenuButton: Shape,
    public val connectionIndicatorBar: Shape,
    public val soundIndicatorBar: Shape,
    public val floatingParticipant: Shape,
    public val connectionQualityIndicator: Shape,
    public val indicatorBackground: Shape,
    val participantLabelShape: Shape,
    val participantContainerShape: Shape,
) {
    public companion object {
        /**
         * Builds the default shapes for our theme.
         *
         * @return A [StreamShapes] that holds our default shapes.
         */
        @Composable
        public fun defaultShapes(): StreamShapes = StreamShapes(
            avatar = CircleShape,
            dialog = RoundedCornerShape(16.dp),
            callButton = CircleShape,
            callControls = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            callControlsLandscape = RectangleShape,
            callControlsButton = CircleShape,
            participantsInfoMenuButton = RoundedCornerShape(32.dp),
            connectionIndicatorBar = RoundedCornerShape(16.dp),
            soundIndicatorBar = RoundedCornerShape(16.dp),
            floatingParticipant = RoundedCornerShape(16.dp),
            connectionQualityIndicator = RoundedCornerShape(topStart = 5.dp),
            indicatorBackground = RoundedCornerShape(5.dp),
            participantLabelShape = RoundedCornerShape(topEnd = 5.dp),
            participantContainerShape = RoundedCornerShape(16.dp),
        )
    }
}
