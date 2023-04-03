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

package io.getstream.video.android.dogfooding

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import io.getstream.video.android.compose.ui.AbstractComposeCallActivity
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.model.state.StreamCallState
import kotlinx.coroutines.launch

class CallActivity : AbstractComposeCallActivity() {

    private val mediaPlayer by lazy {
        MediaPlayer.create(this, R.raw.ringing).apply { isLooping = true }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                getStreamVideo(this@CallActivity).callState.collect { call ->
                    when (call) {
                        is StreamCallState.Joining -> mediaPlayer.start()
                        is StreamCallState.Connected -> mediaPlayer.stop()
                        else -> Unit
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()

        mediaPlayer.stop()
        mediaPlayer.release()
    }

    /**
     * Provides the StreamVideo instance through the videoApp.
     */
    override fun getStreamVideo(context: Context): StreamVideo = context.dogfoodingApp.streamVideo
}
