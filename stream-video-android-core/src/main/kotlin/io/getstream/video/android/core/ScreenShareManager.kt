/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.media.projection.MediaProjection
import android.os.IBinder
import androidx.core.content.ContextCompat
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.screenshare.StreamScreenShareService
import io.getstream.video.android.core.utils.mapState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.webrtc.EglBase
import org.webrtc.ScreenCapturerAndroid
import org.webrtc.SurfaceTextureHelper
import stream.video.sfu.models.VideoDimension

class ScreenShareManager(
    val mediaManager: MediaManagerImpl,
    val eglBaseContext: EglBase.Context,
) {

    companion object {
        // TODO: This could be configurable by the client
        internal val screenShareResolution = VideoDimension(1920, 1080)
        internal val screenShareBitrate = 1_000_000
        internal val screenShareFps = 15
    }

    private val logger by taggedLogger("Media:ScreenShareManager")

    private val _status = MutableStateFlow<DeviceStatus>(DeviceStatus.NotSelected)
    val status: StateFlow<DeviceStatus> = _status

    public val isEnabled: StateFlow<Boolean> = _status.mapState { it is DeviceStatus.Enabled }

    private lateinit var screenCapturerAndroid: ScreenCapturerAndroid
    internal lateinit var surfaceTextureHelper: SurfaceTextureHelper
    private var setupCompleted = false
    private var isScreenSharing = false
    private var mediaProjectionPermissionResultData: Intent? = null

    /**
     * The [ServiceConnection.onServiceConnected] is called when our [StreamScreenShareService]
     * has started. At this point we can start screen-sharing. Starting the screen-sharing without
     * waiting for the Service to start would throw an exception (
     */
    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            if (isScreenSharing) {
                logger.w { "We are already screen-sharing - ignoring call to start another screenshare" }
                return
            }

            // Create the ScreenCapturerAndroid from webrtc-android
            screenCapturerAndroid =
                ScreenCapturerAndroid(
                    mediaProjectionPermissionResultData,
                    object : MediaProjection.Callback() {
                        override fun onStop() {
                            super.onStop()
                            // User can also disable screen sharing from the system menu
                            disable()
                        }
                    },
                )

            // initialize it
            screenCapturerAndroid.initialize(
                surfaceTextureHelper,
                mediaManager.context,
                mediaManager.screenShareVideoSource.capturerObserver,
            )

            // start
            screenCapturerAndroid.startCapture(
                screenShareResolution.width,
                screenShareResolution.height,
                0,
            )

            isScreenSharing = true
        }

        override fun onServiceDisconnected(name: ComponentName) {}
    }

    fun enable(mediaProjectionPermissionResultData: Intent, fromUser: Boolean = true) {
        mediaManager.screenShareTrack.setEnabled(true)
        if (fromUser) {
            _status.value = DeviceStatus.Enabled
        }
        setup()
        startScreenShare(mediaProjectionPermissionResultData)
    }

    fun disable(fromUser: Boolean = true) {
        if (fromUser) {
            _status.value = DeviceStatus.Disabled
        }

        if (isScreenSharing) {
            mediaManager.screenShareTrack.setEnabled(false)
            screenCapturerAndroid.stopCapture()
            mediaManager.context.stopService(
                Intent(mediaManager.context, StreamScreenShareService::class.java),
            )
            isScreenSharing = false
        }
    }

    private fun startScreenShare(mediaProjectionPermissionResultData: Intent) {
        mediaManager.scope.launch {
            this@ScreenShareManager.mediaProjectionPermissionResultData =
                mediaProjectionPermissionResultData

            // Screen sharing requires a foreground service with foregroundServiceType "mediaProjection" to be started first.
            // We can wait for the service to be ready by binding to it and then starting the
            // media projection in onServiceConnected.
            val intent = StreamScreenShareService.createIntent(
                mediaManager.context,
                mediaManager.call.cid,
            )
            ContextCompat.startForegroundService(
                mediaManager.context,
                StreamScreenShareService.createIntent(mediaManager.context, mediaManager.call.cid),
            )
            mediaManager.context.bindService(intent, connection, 0)
        }
    }

    private fun setup() {
        if (setupCompleted) {
            return
        }

        surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBaseContext)

        setupCompleted = true
    }
}
