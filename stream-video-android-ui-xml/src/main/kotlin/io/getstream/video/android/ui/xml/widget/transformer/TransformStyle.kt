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

package io.getstream.video.android.ui.xml.widget.transformer

import io.getstream.video.android.ui.xml.widget.avatar.AvatarStyle
import io.getstream.video.android.ui.xml.widget.call.CallDetailsStyle
import io.getstream.video.android.ui.xml.widget.control.CallControlsStyle
import io.getstream.video.android.ui.xml.widget.control.ControlButtonStyle
import io.getstream.video.android.ui.xml.widget.incoming.IncomingCallStyle
import io.getstream.video.android.ui.xml.widget.outgoing.OutgoingCallStyle

public object TransformStyle {
    @JvmStatic
    public var avatarStyleTransformer: StyleTransformer<AvatarStyle> = noopTransformer()
    @JvmStatic
    public var callDetailsStyleTransformer: StyleTransformer<CallDetailsStyle> = noopTransformer()
    @JvmStatic
    public var outgoingCallStyleTransformer: StyleTransformer<OutgoingCallStyle> = noopTransformer()
    @JvmStatic
    public var incomingCallStyleTransformer: StyleTransformer<IncomingCallStyle> = noopTransformer()
    @JvmStatic
    public var callControlsStyleTransformer: StyleTransformer<CallControlsStyle> = noopTransformer()
    @JvmStatic
    public var controlButtonStyleTransformer: StyleTransformer<ControlButtonStyle> = noopTransformer()

    private fun <T> noopTransformer() = StyleTransformer<T> { it }
}

public fun interface StyleTransformer<T> {
    public fun transform(source: T): T
}
