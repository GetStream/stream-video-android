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

package io.getstream.video.android.service

import android.app.NotificationManager
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.content.ContextCompat
import io.getstream.logging.StreamLog
import io.getstream.video.android.R
import io.getstream.video.android.StreamVideo
import io.getstream.video.android.dispatchers.DispatcherProvider
import io.getstream.video.android.input.CallServiceInput
import io.getstream.video.android.service.notification.StreamNotificationBuilder
import io.getstream.video.android.service.notification.StreamNotificationBuilderImpl
import io.getstream.video.android.service.vibro.StreamVibroManager
import io.getstream.video.android.service.vibro.StreamVibroManagerImpl
import io.getstream.video.android.utils.notificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import io.getstream.video.android.model.state.StreamCallState as State

public abstract class StreamCallService : Service() {

    private val logger = StreamLog.getLogger(TAG)

    private val scope = CoroutineScope(DispatcherProvider.Default)

    private val notificationManager: NotificationManager by lazy { application.notificationManager }

    private val streamVideo: StreamVideo by lazy { getStreamCalls(this) }

    private val notificationBuilder: StreamNotificationBuilder by lazy {
        createNotificationBuilder(application)
    }

    private val vibroManager: StreamVibroManager by lazy { createVibroManager(application) }

    private var curState: State = State.Idle

    protected abstract fun getStreamCalls(context: Context): StreamVideo

    protected open fun createNotificationBuilder(context: Context): StreamNotificationBuilder =
        StreamNotificationBuilderImpl(context, streamVideo, scope) {
            R.id.stream_call_notification
        }

    protected open fun createVibroManager(context: Context): StreamVibroManager =
        StreamVibroManagerImpl(context)

    override fun onCreate() {
        super.onCreate()
        val state = streamVideo.callState.value
        if (state !is State.Active) {
            logger.w { "[onCreate] rejected (state is not Active): $state" }
            destroySelf()
            return
        }
        logger.i { "[onCreate] state: $state" }
        startForeground(state)
        scope.launch {
            streamVideo.callState.collect {
                handleState(it)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        logger.i { "[onDestroy] state: $curState" }
        vibroManager.cancel()
        scope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForeground(state: State.Active) {
        logger.v { "[startForeground] state: $state" }
        val (notificationId, notification) = notificationBuilder.build(state)
        startForeground(notificationId, notification)
    }

    private fun handleState(state: State) {
        logger.v { "[handleState] state: $state" }
        when (state) {
            is State.Idle -> {
                logger.i { "[handleState] state is Idle" }
                destroySelf()
            }
            is State.Active -> {
                updateNotification(state)
                when (state) {
                    is State.Incoming -> when (!state.acceptedByMe) {
                        true -> vibroManager.vibrateIncoming()
                        else -> vibroManager.cancel()
                    }
                    else -> {}
                }
            }
        }
        curState = state
    }

    private fun destroySelf() {
        logger.i { "[destroySelf] no args" }
        stopSelf()
    }

    private fun updateNotification(state: State.Active) {
        logger.v { "[updateNotification] state: $state" }
        val (notificationId, notification) = notificationBuilder.build(state)
        notificationManager.notify(notificationId, notification)
    }

    public companion object {
        @PublishedApi
        internal const val TAG: String = "Call:StreamService"

        public fun start(context: Context, input: CallServiceInput) {
            StreamLog.v(TAG) { "/start/ service: ${input.serviceClassName}" }
            ContextCompat.startForegroundService(
                context, newIntent(context, input)
            )
        }

        public fun newIntent(context: Context, input: CallServiceInput): Intent {
            StreamLog.v(TAG) { "/newIntent/ service: ${input.serviceClassName}" }
            return Intent().apply {
                component = ComponentName(context.packageName, input.serviceClassName)
            }
        }

        public inline fun <reified T : StreamCallService> start(context: Context) {
            StreamLog.v(TAG) { "/start/ service: ${T::class}" }
            ContextCompat.startForegroundService(
                context, Intent(context, T::class.java)
            )
        }

        public inline fun <reified T : StreamCallService> stop(context: Context) {
            StreamLog.v(TAG) { "/stop/ service: ${T::class}" }
            context.stopService(
                Intent(context, T::class.java)
            )
        }
    }
}
