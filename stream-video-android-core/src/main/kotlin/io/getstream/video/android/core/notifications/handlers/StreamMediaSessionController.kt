/*
 * Copyright (c) 2014-2026 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core.notifications.handlers

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.MediaSessionCompat.Callback
import android.support.v4.media.session.PlaybackStateCompat
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.internal.ExperimentalStreamVideoApi
import io.getstream.video.android.model.StreamCallId
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap

/**
 * Controller for managing media sessions.
 */
@ExperimentalStreamVideoApi
interface StreamMediaSessionController {

    /**
     * Create or get already created media session for the call.
     *
     * @param application The application context.
     * @param callId The call id.
     * @param channelId The channel id.
     */
    fun provideMediaSession(
        application: Application,
        callId: StreamCallId,
        channelId: String,
        callback: MediaSessionCompat.Callback?,
    ): MediaSessionCompat

    /**
     * Create the media session metadata.
     *
     * @param callId The call id.
     * @param metadataBuilder The metadata builder.
     */
    fun initialMetadata(
        context: Context,
        mediaSession: MediaSessionCompat,
        callId: StreamCallId,
        metadataBuilder: MediaMetadataCompat.Builder,
    )

    /**
     * Create the media session playback state.
     *
     * @param callId The call id.
     * @param playbackStateBuilder The playback state builder.
     */
    fun initialPlaybackState(
        context: Context,
        mediaSession: MediaSessionCompat,
        callId: StreamCallId,
        playbackStateBuilder: PlaybackStateCompat.Builder,
    )

    /**
     * Update the media session metadata.
     *
     * @param mediaSession The media session.
     * @param call The call.
     * @param callDisplayName The call display name.
     * @param metadataBuilder The metadata builder.
     */
    suspend fun updateMetadata(
        context: Context,
        mediaSession: MediaSessionCompat,
        call: Call,
        callDisplayName: String?,
        metadataBuilder: MediaMetadataCompat.Builder,
    )

    /**
     * Update the media session playback state.
     *
     * @param mediaSession The media session.
     * @param call The call.
     * @param callDisplayName The call display name.
     * @param playbackStateBuilder The playback state builder.
     */
    suspend fun updatePlaybackState(
        context: Context,
        mediaSession: MediaSessionCompat,
        call: Call,
        callDisplayName: String?,
        playbackStateBuilder: PlaybackStateCompat.Builder,
    )

    /**
     * Clear the media session for the call.
     *
     * @param callId The call id.
     */
    fun clear(callId: StreamCallId)
}

/**
 * Default implementation of the [StreamMediaSessionController] interface.
 */
@ExperimentalStreamVideoApi
open class DefaultStreamMediaSessionController(
    private val interceptors: StreamNotificationBuilderInterceptors,
    private val updateInterceptors: StreamNotificationUpdateInterceptors,
) : StreamMediaSessionController {

    private val logger by taggedLogger("Video:MediaSessionController")
    private val mediaSessions: ConcurrentHashMap<String, MediaSessionCompat> =
        ConcurrentHashMap()

    override fun provideMediaSession(
        application: Application,
        callId: StreamCallId,
        channelId: String,
        callback: Callback?,
    ): MediaSessionCompat {
        val resolvedMediaSession = mediaSessions[callId.cid]

        if (resolvedMediaSession != null) {
            return resolvedMediaSession
        }

        val mediaSession = mediaSessions[callId.cid] ?: interceptors.onCreateMediaSessionCompat(
            application,
            channelId,
        ) ?: MediaSessionCompat(application, channelId)

        val metadata = interceptors.onBuildMediaNotificationMetadata(
            MediaMetadataCompat.Builder(),
            callId,
        ).build()
        mediaSession.setMetadata(metadata)

        val playbackState = interceptors.onBuildMediaNotificationPlaybackState(
            PlaybackStateCompat.Builder(),
            callId,
        ).build()
        mediaSession.setPlaybackState(playbackState)

        if (callback != null) {
            mediaSession.setCallback(
                object : Callback() {
                    override fun onPlay() {
                        callback.onPlay()
                        super.onPlay()
                        val client = StreamVideo.instanceOrNull() as? StreamVideoClient
                        client?.scope?.launch {
                            runBlocking {
                                val call = StreamVideo.instanceOrNull()?.state?.activeCall?.value
                                if (call != null) {
                                    updatePlaybackState(
                                        application,
                                        mediaSession,
                                        call,
                                        null,
                                        PlaybackStateCompat.Builder(),
                                    )
                                }
                            }
                        }
                    }

                    override fun onPause() {
                        callback.onPause()
                        super.onPause()
                        val client = StreamVideo.instanceOrNull() as? StreamVideoClient
                        client?.scope?.launch {
                            runBlocking {
                                val call = StreamVideo.instanceOrNull()?.state?.activeCall?.value
                                if (call != null) {
                                    updatePlaybackState(
                                        application,
                                        mediaSession,
                                        call,
                                        null,
                                        PlaybackStateCompat.Builder(),
                                    )
                                }
                            }
                        }
                    }
                },
                Handler(Looper.getMainLooper()),
            )
        }

        mediaSessions[callId.cid] = mediaSession

        return mediaSession
    }

    override fun initialMetadata(
        context: Context,
        mediaSession: MediaSessionCompat,
        callId: StreamCallId,
        metadataBuilder: MediaMetadataCompat.Builder,
    ) {
        logger.d { "[initialMetadata] Updating media metadata for session: $mediaSession" }
        metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, -1L)
        val intercepted = interceptors.onBuildMediaNotificationMetadata(metadataBuilder, callId)
        val metadata = intercepted.build()
        mediaSession.setMetadata(metadata)
    }

    override fun initialPlaybackState(
        context: Context,
        mediaSession: MediaSessionCompat,
        callId: StreamCallId,
        playbackStateBuilder: PlaybackStateCompat.Builder,
    ) {
        logger.d { "[initialPlaybackState] Updating media metadata for session: $mediaSession" }
        playbackStateBuilder.setState(
            PlaybackStateCompat.STATE_NONE,
            PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
            1f,
        )
        val intercepted = interceptors.onBuildMediaNotificationPlaybackState(
            playbackStateBuilder,
            callId,
        )
        val playbackState = intercepted.build()
        mediaSession.setPlaybackState(playbackState)
    }

    override suspend fun updateMetadata(
        context: Context,
        mediaSession: MediaSessionCompat,
        call: Call,
        callDisplayName: String?,
        metadataBuilder: MediaMetadataCompat.Builder,
    ) {
        logger.d { "[updateMetadata] Updating media metadata for session: $mediaSession" }
        metadataBuilder.putLong(
            MediaMetadataCompat.METADATA_KEY_DURATION,
            PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
        )
        val interceptedInitial = interceptors.onBuildMediaNotificationMetadata(
            metadataBuilder,
            StreamCallId.fromCallCid(call.cid),
        )
        val intercepted = updateInterceptors.onUpdateMediaNotificationMetadata(
            interceptedInitial,
            call,
            callDisplayName,
        )
        mediaSession.setMetadata(intercepted.build())
    }

    override suspend fun updatePlaybackState(
        context: Context,
        mediaSession: MediaSessionCompat,
        call: Call,
        callDisplayName: String?,
        playbackStateBuilder: PlaybackStateCompat.Builder,
    ) {
        logger.d { "[updatePlaybackState] Updating media metadata for session: $mediaSession" }
        val isPlaying = call.session?.subscriber?.isEnabled()?.value
        val playbackState = if (isPlaying == true) {
            PlaybackStateCompat.STATE_PLAYING
        } else if (isPlaying == false) {
            PlaybackStateCompat.STATE_PAUSED
        } else {
            PlaybackStateCompat.STATE_NONE
        }
        val durationInMs = call.state.duration.value?.inWholeMilliseconds
        playbackStateBuilder.setState(
            playbackState,
            durationInMs ?: PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
            1f,
        )

        val intercepted = updateInterceptors.onUpdateMediaNotificationPlaybackState(
            playbackStateBuilder,
            call,
            callDisplayName,
        )
        mediaSession.setPlaybackState(intercepted.build())
    }

    override fun clear(callId: StreamCallId) {
        logger.d { "[clear] Clearing media session for call: $callId" }
        mediaSessions.remove(callId.cid)
    }
}
