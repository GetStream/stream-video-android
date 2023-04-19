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
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.core.os.bundleOf
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.CallContainer
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.viewmodel.CallViewModel
import io.getstream.video.android.core.viewmodel.CallViewModelFactory

class CallActivity : AppCompatActivity() {

    private val streamVideo: StreamVideo by lazy { dogfoodingApp.streamVideo }
    private val factory by lazy { callViewModelFactory() }
    private val vm by viewModels<CallViewModel> { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            VideoTheme {
                CallContainer(
                    modifier = Modifier.background(color = VideoTheme.colors.appBackground),
                    callViewModel = vm,
                    onBackPressed = { finish() },
                )
            }
        }
    }

    private fun callViewModelFactory(): CallViewModelFactory {
        val type = intent.getStringExtra(EXTRA_TYPE)
            ?: throw IllegalArgumentException("You must pass correct call type.")
        val id = intent.getStringExtra(EXTRA_ID)
            ?: throw IllegalArgumentException("You must pass correct call id.")

        return CallViewModelFactory(
            streamVideo = streamVideo,
            call = streamVideo.call(type = type, id = id),
            permissionManager = null
        )
    }

    companion object {
        internal const val EXTRA_TYPE = "type"
        internal const val EXTRA_ID = "id"

        fun getIntent(context: Context, type: String, id: String): Intent {
            return Intent(context, CallActivity::class.java).apply {
                putExtras(
                    bundleOf(EXTRA_TYPE to type, EXTRA_ID to id)
                )
            }
        }
    }
}
