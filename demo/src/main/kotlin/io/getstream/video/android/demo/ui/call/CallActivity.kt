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

package io.getstream.video.android.demo.ui.call

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import io.getstream.video.android.common.AbstractCallActivity
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.activecall.CallContent
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.model.StreamCallId
import kotlinx.coroutines.launch

class CallActivity : AbstractCallActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            call.join(create = true)
        }

        setContent {
            VideoTheme {
                CallContent(
                    modifier = Modifier.background(color = VideoTheme.colors.appBackground),
                    call = call,
                    onBackPressed = { finish() },
                )
            }
        }
    }

    override fun provideCall(): Call {
        val streamVideo = StreamVideo.instance()
        val cid = intent.getParcelableExtra<StreamCallId>(EXTRA_CID)
            ?: throw IllegalArgumentException("call type and id is invalid!")
        return streamVideo.call(cid.type, cid.id)
    }

    override fun pipChanged(isInPip: Boolean) {
    }
}
