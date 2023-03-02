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

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import io.getstream.video.android.app.BuildConfig
import io.getstream.video.android.app.user.FakeUsersProvider
import io.getstream.video.android.app.videoApp
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.AbstractComposeCallActivity
import io.getstream.video.android.compose.ui.components.video.VideoRenderer
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.user.EmptyUsersProvider
import io.getstream.video.android.core.viewmodel.CallViewModelFactory
import stream.video.sfu.models.TrackType

class CallActivity : AbstractComposeCallActivity() {

    /**
     * Provides the StreamVideo instance through the videoApp.
     */
    override fun getStreamVideo(context: Context): StreamVideo = context.videoApp.streamVideo

    /**
     * Provides a custom factory for the ViewModel, that provides fake users for invites.
     */
    override fun getCallViewModelFactory(): CallViewModelFactory {
        val currentUserId = getStreamVideo(this).getUser().id

        return CallViewModelFactory(
            streamVideo = getStreamVideo(this),
            permissionManager = getPermissionManager(),
            usersProvider = if (BuildConfig.DEBUG) FakeUsersProvider(currentUserId) else EmptyUsersProvider
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val callState by callViewModel.callState.collectAsState()
            val call = callState

            VideoTheme {
                if (call != null) {
                    val participants by call.callParticipants.collectAsState()
                    val localParticipant = participants.firstOrNull { it.isLocal }

                    val localVideoTrack = localParticipant?.videoTrack
                    val sessionId = localParticipant?.sessionId ?: ""

                    if (localVideoTrack != null) {
                        VideoRenderer(
                            modifier = Modifier.fillMaxSize(),
                            call = call,
                            videoTrack = localVideoTrack,
                            sessionId = sessionId,
                            trackType = TrackType.TRACK_TYPE_VIDEO
                        )
                    }
                }
            }
        }
    }
}
