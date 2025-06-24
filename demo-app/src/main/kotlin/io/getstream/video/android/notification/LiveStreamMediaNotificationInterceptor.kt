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

package io.getstream.video.android.notification

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media.session.MediaButtonReceiver
import io.getstream.video.android.R
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.notifications.handlers.StreamNotificationUpdateInterceptors
import java.net.URL

class LiveStreamMediaNotificationInterceptor(private val context: Context) : StreamNotificationUpdateInterceptors() {
    private var bitmap: Bitmap? = null

    override suspend fun onUpdateMediaNotificationPlaybackState(
        builder: PlaybackStateCompat.Builder,
        call: Call,
        callDisplayName: String?,
    ): PlaybackStateCompat.Builder {
        kotlinx.coroutines.delay(6000)
        builder.setActions(
            PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE,
        )
        return builder
    }

    override suspend fun onUpdateOngoingCallMediaNotification(
        builder: NotificationCompat.Builder,
        callDisplayName: String?,
        call: Call,
    ): NotificationCompat.Builder {
        val bitmap = getStreamLogoBitmap()
        if (bitmap != null) {
            builder.setLargeIcon(bitmap)
        }

        val playAction = NotificationCompat.Action(
            android.R.drawable.ic_media_play,
            "Play",
            MediaButtonReceiver.buildMediaButtonPendingIntent(
                context,
                PlaybackStateCompat.ACTION_PLAY,
            ),
        )

        val pauseAction = NotificationCompat.Action(
            android.R.drawable.ic_media_pause,
            "Pause",
            MediaButtonReceiver.buildMediaButtonPendingIntent(
                context,
                PlaybackStateCompat.ACTION_PAUSE,
            ),
        )
        builder.addAction(playAction)
        builder.addAction(pauseAction)

        return builder
    }

    override suspend fun onUpdateMediaNotificationMetadata(
        builder: MediaMetadataCompat.Builder,
        call: Call,
        callDisplayName: String?,
    ): MediaMetadataCompat.Builder {
        val bitmap = getStreamLogoBitmap()
        if (bitmap != null) {
            Log.d("StreamVideoInitHelper", "Loaded image")
            builder.putBitmap(
                MediaMetadataCompat.METADATA_KEY_ALBUM_ART,
                bitmap,
            )
        }
        return builder
    }

    private fun getStreamLogoBitmap(): Bitmap? {
        if (bitmap == null) {
            bitmap = try {
                URL("https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQ8dzj-6rSfEfYMOXPSCV3s84Luuqr2c9KzMg&s").openStream()
                    .use { BitmapFactory.decodeStream(it) }
            } catch (e: Exception) {
                // Fallback
                BitmapFactory.decodeResource(context.resources, R.drawable.stream_calls_logo)
            }
        }
        return bitmap
    }
}
