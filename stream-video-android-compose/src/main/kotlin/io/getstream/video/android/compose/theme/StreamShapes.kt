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
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape

/**
 * Contains all the shapes we provide for our components.
 *
 * @param callButton The avatar shape.
 */
@Immutable
public data class StreamShapes(
    public val avatar: Shape,
    public val callButton: Shape,
) {
    public companion object {
        /**
         * Builds the default shapes for our theme.
         *
         * @return A [StreamShapes] that holds our default shapes.
         */
        public fun defaultShapes(): StreamShapes = StreamShapes(
            avatar = CircleShape,
            callButton = CircleShape,
        )
    }
}
