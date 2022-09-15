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

package io.getstream.video.android.app.ui.test

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraMetadata.LENS_FACING_FRONT
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.getstream.video.android.app.R
import io.getstream.video.android.app.VideoApp
import io.getstream.video.android.model.Room
import kotlinx.coroutines.launch
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack
import java.util.*

class SfuTestActivity : AppCompatActivity() {

    private val videoClient by lazy {
        VideoApp.videoClient
    }

    private val webRTCClient by lazy {
        videoClient.webRTCClient
    }

    private lateinit var room: Room

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sfu_test)
        testWebRTCClient()
        observeParticipants()
    }

    private fun observeParticipants() {
        lifecycleScope.launch {
            room.callParticipants.collect { participants ->
                val user = videoClient.getUser()
                val participant = participants.firstOrNull { it.track != null && it.id != user.id }
                val videoTrack = participant?.track

                if (videoTrack != null) {
                    val participantView = findViewById<SurfaceViewRenderer>(R.id.participantView)

                    room.initRenderer(participantView, videoTrack.streamId)
                    videoTrack.video.addSink(participantView)
                }
            }
        }
    }

    private fun testWebRTCClient() {
        val room = webRTCClient.joinCall(UUID.randomUUID().toString(), true)
        this.room = room
        room.onLocalVideoTrackChange = ::updateVideoTrack
    }

    private fun updateVideoTrack(videoTrack: VideoTrack) {
        val renderer = findViewById<SurfaceViewRenderer>(R.id.surfaceView)
        room.initRenderer(renderer, videoTrack.id())

        webRTCClient.startCapturingLocalVideo(LENS_FACING_FRONT)
        videoTrack.addSink(renderer)
    }

    companion object {
        fun getIntent(context: Context): Intent = Intent(context, SfuTestActivity::class.java)
    }
}
