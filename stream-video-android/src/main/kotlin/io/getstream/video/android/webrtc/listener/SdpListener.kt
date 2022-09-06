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

package io.getstream.video.android.webrtc.listener

import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

public class SdpListener(
    private inline val createSuccess: (SessionDescription?) -> Unit = {},
    private inline val setSuccess: () -> Unit = {},
    private inline val createFailure: (String?) -> Unit = {},
    private inline val setFailure: (String?) -> Unit = {}
) : SdpObserver {
    override fun onCreateSuccess(description: SessionDescription?): Unit = createSuccess(description)

    override fun onSetSuccess(): Unit = setSuccess()

    override fun onCreateFailure(message: String?): Unit = createFailure(message)

    override fun onSetFailure(message: String?): Unit = setFailure(message)
}
