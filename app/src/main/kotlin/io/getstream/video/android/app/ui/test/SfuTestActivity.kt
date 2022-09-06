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
import kotlinx.coroutines.launch
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

class SfuTestActivity : AppCompatActivity() {

    private val videoClient by lazy {
        VideoApp.videoClient
    }

    private val webRTCClient by lazy {
        videoClient.webRTCClient
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sfu_test)
        val renderer = findViewById<SurfaceViewRenderer>(R.id.surfaceView)
        val participantView = findViewById<SurfaceViewRenderer>(R.id.participantView)

        renderer.init(webRTCClient.eglBase.eglBaseContext, null)
        participantView.init(webRTCClient.eglBase.eglBaseContext, null)

        observeParticipants()
        testWebRTCClient()
    }

    private fun observeParticipants() {
        lifecycleScope.launch {
            webRTCClient.callParticipants.collect { participants ->
                val user = videoClient.getUser()
                val participant = participants.firstOrNull { it.track != null && it.id != user.id }

                if (participant != null) {
                    val participantView = findViewById<SurfaceViewRenderer>(R.id.participantView)

                    participant.track?.addSink(participantView)
                }
            }
        }
    }

    private fun testWebRTCClient() {
        webRTCClient.onLocalVideoTrackChange = ::updateVideoTrack

        webRTCClient.connect(true)
    }

    private fun updateVideoTrack(videoTrack: VideoTrack) {
        val renderer = findViewById<SurfaceViewRenderer>(R.id.surfaceView)

        webRTCClient.startCapturingLocalVideo(LENS_FACING_FRONT)
        videoTrack.addSink(renderer)
    }

    companion object {
        fun getIntent(context: Context): Intent = Intent(context, SfuTestActivity::class.java)
    }
}
