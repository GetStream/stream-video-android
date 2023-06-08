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

package io.getstream.video.android.app.ui.call

import io.getstream.video.android.common.AbstractCallActivity
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.model.StreamCallId

class XmlCallActivity : AbstractCallActivity() {

    override fun createCall(): Call {
        val streamVideo = StreamVideo.instance()
        val cid = intent.getParcelableExtra<StreamCallId>(EXTRA_CID)
            ?: throw IllegalArgumentException("call type and id is invalid!")
        return streamVideo.call(cid.type, cid.id)
    }

    override fun closeCall() {
        createCall().leave()
    }

    override fun pipChanged(isInPip: Boolean) {
    }
}
