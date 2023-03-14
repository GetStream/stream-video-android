/*
 * Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.common.viewmodel

import android.app.Application
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import androidx.core.content.getSystemService
import androidx.lifecycle.AndroidViewModel
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.call.connection.StreamPeerConnectionFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.webrtc.Camera2Capturer
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraEnumerator
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoCapturer
import org.webrtc.VideoSource

public class CallLobbyViewModel(
    application: Application,
    private val streamVideo: StreamVideo
) : AndroidViewModel(application) {

    private val peerConnectionFactory: StreamPeerConnectionFactory by lazy {
        StreamPeerConnectionFactory(application)
    }

    private val eglBase by lazy {
        peerConnectionFactory.eglBase
    }

    private val cameraManager by lazy { application.getSystemService<CameraManager>() }
    private val cameraEnumerator: CameraEnumerator by lazy {
        Camera2Enumerator(application)
    }
    private val surfaceTextureHelper by lazy {
        SurfaceTextureHelper.create(
            "CaptureThread", eglBase.eglBaseContext
        )
    }

    private val videoCapturer: VideoCapturer? by lazy { buildCameraCapturer() }

    private lateinit var videoSource: VideoSource

    private val _isCapturingVideo: MutableStateFlow<Boolean> = MutableStateFlow(false)
    public val isCapturingVideo: StateFlow<Boolean> = _isCapturingVideo

    private val _isCapturingAudio: MutableStateFlow<Boolean> = MutableStateFlow(false)
    public val isCapturingAudio: StateFlow<Boolean> = _isCapturingVideo

    private fun buildCameraCapturer(): VideoCapturer? {
        val manager = cameraManager ?: return null

        val ids = manager.cameraIdList
        var foundCamera = false
        var cameraId = ""

        for (id in ids) {
            val characteristics = manager.getCameraCharacteristics(id)
            val cameraLensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)

            if (cameraLensFacing == CameraMetadata.LENS_FACING_FRONT) {
                foundCamera = true
                cameraId = id
            }
        }

        if (!foundCamera && ids.isNotEmpty()) {
            cameraId = ids.first()
        }

        return Camera2Capturer(getApplication(), cameraId, null)
    }

    public fun flipCamera() {
        (videoCapturer as? Camera2Capturer)?.switchCamera(null)
    }

    public fun startCapturingLocalVideo(position: Int) {
        val capturer = videoCapturer as? Camera2Capturer ?: return
        val enumerator = cameraEnumerator as? Camera2Enumerator ?: return

        val frontCamera = enumerator.deviceNames.first {
            if (position == 0) {
                enumerator.isFrontFacing(it)
            } else {
                enumerator.isBackFacing(it)
            }
        }

        val supportedFormats = enumerator.getSupportedFormats(frontCamera) ?: emptyList()

        val resolution = supportedFormats.firstOrNull {
            (it.width == 720 || it.width == 480 || it.width == 360)
        } ?: return

        capturer.startCapture(resolution.width, resolution.height, 30)
        _isCapturingVideo.value = true

        videoSource = peerConnectionFactory.makeVideoSource(false)

        capturer.initialize(surfaceTextureHelper, getApplication(), videoSource.capturerObserver)
    }

    private fun joinCallLobby() {
        // TODO - join call lobby
    }
}
