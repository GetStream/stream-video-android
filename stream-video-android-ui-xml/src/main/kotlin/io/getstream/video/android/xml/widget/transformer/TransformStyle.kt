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

package io.getstream.video.android.xml.widget.transformer

import io.getstream.video.android.xml.widget.appbar.CallAppBarStyle
import io.getstream.video.android.xml.widget.avatar.AvatarStyle
import io.getstream.video.android.xml.widget.call.CallViewStyle
import io.getstream.video.android.xml.widget.callcontainer.CallContainerStyle
import io.getstream.video.android.xml.widget.calldetails.CallDetailsStyle
import io.getstream.video.android.xml.widget.control.CallControlsStyle
import io.getstream.video.android.xml.widget.control.ControlButtonStyle
import io.getstream.video.android.xml.widget.incoming.IncomingCallStyle
import io.getstream.video.android.xml.widget.outgoing.OutgoingCallStyle
import io.getstream.video.android.xml.widget.participant.CallParticipantStyle
import io.getstream.video.android.xml.widget.participant.PictureInPictureStyle
import io.getstream.video.android.xml.widget.screenshare.ScreenShareStyle

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
    public var controlButtonStyleTransformer: StyleTransformer<ControlButtonStyle> =
        noopTransformer()

    @JvmStatic
    public var callParticipantStyleTransformer: StyleTransformer<CallParticipantStyle> =
        noopTransformer()

    @JvmStatic
    public var callViewStyleTransformer: StyleTransformer<CallViewStyle> = noopTransformer()

    @JvmStatic
    public var pictureInPictureStyleTransformer: StyleTransformer<PictureInPictureStyle> =
        noopTransformer()

    @JvmStatic
    public var screenShareStyleTransformer: StyleTransformer<ScreenShareStyle> = noopTransformer()

    @JvmStatic
    public var callAppBarStyleTransformer: StyleTransformer<CallAppBarStyle> = noopTransformer()

    @JvmStatic
    public var callContainerStyleTransformer: StyleTransformer<CallContainerStyle> =
        noopTransformer()

    private fun <T> noopTransformer() = StyleTransformer<T> { it }
}

public fun interface StyleTransformer<T> {
    public fun transform(source: T): T
}
