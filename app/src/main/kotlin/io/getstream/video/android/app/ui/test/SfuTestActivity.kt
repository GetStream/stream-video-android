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
import io.getstream.video.android.app.R
import io.getstream.video.android.app.VideoApp
import org.webrtc.SurfaceViewRenderer

class SfuTestActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sfu_test)
        testWebRTCClient()
    }

    private fun testWebRTCClient() {
        val client = VideoApp.videoClient.webRTCClient

        client.connect(true)
        val renderer = findViewById<SurfaceViewRenderer>(R.id.surfaceView)

        client.startCapturingLocalVideo(renderer, LENS_FACING_FRONT)
    }

    companion object {
        fun getIntent(context: Context): Intent = Intent(context, SfuTestActivity::class.java)
    }
}
