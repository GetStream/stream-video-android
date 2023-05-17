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

package io.getstream.video.android.core.service

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import io.getstream.log.taggedLogger
import io.getstream.video.android.R
import io.getstream.video.android.core.ConnectionState
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.dispatchers.DispatcherProvider
import io.getstream.video.android.core.service.notification.StreamNotificationBuilder
import io.getstream.video.android.core.service.notification.internal.StreamNotificationBuilderImpl
import io.getstream.video.android.core.service.vibro.StreamVibroManager
import io.getstream.video.android.core.service.vibro.StreamVibroManagerImpl
import io.getstream.video.android.core.utils.notificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel

public abstract class AbstractStreamCallService : Service() {

    private val logger by taggedLogger("Call:StreamService")

    private val scope = CoroutineScope(DispatcherProvider.Default)

    private val notificationManager: NotificationManager by lazy { application.notificationManager }

    private val streamVideo: StreamVideo by lazy { StreamVideo.instance() }

    private val notificationBuilder: StreamNotificationBuilder by lazy {
        createNotificationBuilder(application)
    }

    private val vibroManager: StreamVibroManager by lazy { createVibroManager(application) }

    private var curState: ConnectionState = ConnectionState.PreConnect

    protected open fun createNotificationBuilder(context: Context): StreamNotificationBuilder =
        StreamNotificationBuilderImpl(context, streamVideo, scope) {
            R.id.stream_video_call_notification
        }

    protected open fun createVibroManager(context: Context): StreamVibroManager =
        StreamVibroManagerImpl(context)

    override fun onCreate() {
        super.onCreate()
        // TODO: FIXME
//        val state = streamVideo.callState.value
//        if (state !is State.Active) {
//            logger.w { "[onCreate] rejected (state is not Active): $state" }
//            destroySelf()
//            return
//        }
//        logger.i { "[onCreate] state: $state" }
//        startForeground(state)
//        scope.launch {
//            streamVideo.callState.collect {
//                handleState(it)
//            }
//        }
    }

    override fun onDestroy() {
        super.onDestroy()
        logger.i { "[onDestroy] state: $curState" }
        vibroManager.cancel()
        scope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForeground(state: ConnectionState) {
        logger.v { "[startForeground] state: $state" }
        val (notificationId, notification) = notificationBuilder.build("", state)
        startForeground(notificationId, notification)
    }

    private fun handleState(state: ConnectionState) {
        logger.v { "[handleState] state: $state" }
//        when (state) {
//            is ConnectionState.PreConnect -> {
//                logger.i { "[handleState] state is Idle" }
//                destroySelf()
//            }
//
//            is ConnectionState.Connected -> {
//                updateNotification(state)
//                when (state) {
//                    is State.Incoming -> when (!state.acceptedByMe) {
//                        true -> vibroManager.vibrateIncoming()
//                        else -> vibroManager.cancel()
//                    }
//                    else -> {}
//                }
//            }
//        }
        curState = state
    }

    private fun destroySelf() {
        logger.i { "[destroySelf] no args" }
        stopSelf()
    }

    private fun updateNotification(state: ConnectionState) {
        logger.v { "[updateNotification] state: $state" }
        val (notificationId, notification) = notificationBuilder.build("", state)
        notificationManager.notify(notificationId, notification)
    }
}
