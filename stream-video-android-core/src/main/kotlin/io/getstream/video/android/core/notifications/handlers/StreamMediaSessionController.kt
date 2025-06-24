package io.getstream.video.android.core.notifications.handlers

import android.app.Application
import android.content.Context
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.session.MediaButtonReceiver
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.internal.ExperimentalStreamVideoApi
import io.getstream.video.android.model.StreamCallId
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
        channelId: String
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
        metadataBuilder: MediaMetadataCompat.Builder
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
        playbackStateBuilder: PlaybackStateCompat.Builder
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
        metadataBuilder: MediaMetadataCompat.Builder
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
        playbackStateBuilder: PlaybackStateCompat.Builder
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
        channelId: String
    ): MediaSessionCompat {
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

        mediaSessions[callId.cid] = mediaSession

        return mediaSession
    }

    override fun initialMetadata(
        context: Context,
        mediaSession: MediaSessionCompat,
        callId: StreamCallId,
        metadataBuilder: MediaMetadataCompat.Builder
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
        playbackStateBuilder: PlaybackStateCompat.Builder
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
        metadataBuilder: MediaMetadataCompat.Builder
    ) {
        logger.d { "[updateMetadata] Updating media metadata for session: $mediaSession" }
        val durationInMs = call.state.duration.value?.inWholeMilliseconds
        metadataBuilder.putLong(
            MediaMetadataCompat.METADATA_KEY_DURATION,
            durationInMs ?: PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN
        )
        val interceptedInitial = interceptors.onBuildMediaNotificationMetadata(
            metadataBuilder,
            StreamCallId.fromCallCid(call.cid),
        )
        val intercepted = updateInterceptors.onUpdateMediaNotificationMetadata(
            interceptedInitial,
            call,
            callDisplayName
        )
        mediaSession.setMetadata(intercepted.build())
    }

    override suspend fun updatePlaybackState(
        context: Context,
        mediaSession: MediaSessionCompat,
        call: Call,
        callDisplayName: String?,
        playbackStateBuilder: PlaybackStateCompat.Builder
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
        val interceptedInitial = interceptors.onBuildMediaNotificationPlaybackState(
            playbackStateBuilder,
            StreamCallId.fromCallCid(call.cid),
        )

        val intercepted = updateInterceptors.onUpdateMediaNotificationPlaybackState(
            interceptedInitial,
            call,
            callDisplayName
        )
        mediaSession.setPlaybackState(intercepted.build())
    }

    override fun clear(callId: StreamCallId) {
        logger.d { "[clear] Clearing media session for call: $callId" }
        mediaSessions.remove(callId.cid)
    }
}

