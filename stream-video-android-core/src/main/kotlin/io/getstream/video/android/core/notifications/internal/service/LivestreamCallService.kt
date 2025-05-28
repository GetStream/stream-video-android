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

package io.getstream.video.android.core.notifications.internal.service

import android.app.Notification
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.session.MediaButtonReceiver
import io.getstream.log.TaggedLogger
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.model.StreamCallId

/**
 * Due to the nature of the livestream calls, the service that is used is of different type.
 */
internal open class LivestreamCallService : CallService() {
    override val logger: TaggedLogger by taggedLogger("LivestreamHostCallService")
    override val serviceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
}

/**
 * Due to the nature of the livestream calls, the service that is used is of different type.
 */
internal open class LivestreamAudioCallService : CallService() {
    override val logger: TaggedLogger by taggedLogger("LivestreamAudioCallService")
    override val serviceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
}

/**
 * Due to the nature of the livestream calls, the service that is used is of different type.
 */
internal class LivestreamViewerService : LivestreamCallService() {
    override val logger: TaggedLogger by taggedLogger("LivestreamViewerService")
    override val serviceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK

    private lateinit var mediaSession: MediaSessionCompat

    override fun onCreate() {
        super.onCreate()
        mediaSession = MediaSessionCompat(this, "LivestreamViewerService").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    super.onPlay()
                    startPlayback()
                }

                override fun onPause() {
                    super.onPause()
                    pausePlayback()
                }

                override fun onStop() {
                    super.onStop()
                    stopSelf()
                }

                override fun onCustomAction(action: String?, extras: Bundle?) {
                    super.onCustomAction(action, extras)
                }
            })
            isActive = true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == Intent.ACTION_MEDIA_BUTTON) {
            MediaButtonReceiver.handleIntent(mediaSession, intent)
            return START_STICKY // Keep service running after handling media button
        }
        startPlayback()
        // For other intents, call the super implementation
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startPlayback() {
        // Start or resume your livestream playback logic
        mediaSession.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_PLAYING, 0, 1f)
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_STOP,
                )
                .build(),
        )
        val livestreamCall = StreamVideo.instance().state.activeCall.value
        livestreamCall?.state?.livestreamAudio?.value?.track?.audio?.setEnabled(true)
        livestreamCall?.state?.livestream?.value?.track?.video?.setEnabled(true)
    }

    private fun pausePlayback() {
        // Pause your livestream playback logic
        mediaSession.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_PAUSED, 0, 1f)
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_STOP,
                )
                .build(),
        )
        val livestreamCall = StreamVideo.instance().state.activeCall.value
        livestreamCall?.state?.livestreamAudio?.value?.track?.audio?.setEnabled(false)
        livestreamCall?.state?.livestream?.value?.track?.video?.setEnabled(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaSession.release()
    }

    override fun getNotificationPair(
        trigger: String,
        streamVideo: StreamVideoClient,
        intentCallId: StreamCallId,
        intentCallDisplayName: String?,
    ): Pair<Notification?, Int> {
        return when (trigger) {
            TRIGGER_ONGOING_CALL -> Pair(
                first = streamVideo.getOngoingCallNotificationLiveStreamViewer( // Noob livestream viewer 1 (ongoing) , livestream viewer 1 (leave)
                    callId = intentCallId,
                    callDisplayName = intentCallDisplayName,
                    isOutgoingCall = false,
                    remoteParticipantCount = 0,
                    mediaSession = mediaSession,
                    context = StreamVideo.instance().context,
                ),
                second = intentCallId.hashCode(),
            ) else -> super.getNotificationPair(
                trigger,
                streamVideo,
                intentCallId,
                intentCallDisplayName,
            )
        }
    }
}
