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

import android.content.Context
import android.os.Build
import io.getstream.video.android.core.IncomingRingtoneOwner
import io.getstream.video.android.core.R
import io.getstream.video.android.core.utils.isAndroid17OrHigher

/**
 * Resolves the incoming-call channel from the ringtone owner selected for the call.
 * Existing notifications keep their current channel because Android does not allow an update to
 * move an actively ringing notification between channels without interrupting its alert.
 */
internal class IncomingCallChannelResolver(
    context: Context,
    private val notificationChannels: StreamNotificationChannels,
    private val sdkInt: Int = Build.VERSION.SDK_INT,
) {
    private val legacyIncomingChannelId =
        context.getString(R.string.stream_video_incoming_call_notification_channel_id)
    private val ringingIncomingChannelId =
        context.getString(R.string.stream_video_incoming_call_ringing_notification_channel_id)

    private val legacyIncomingLowImportanceChannelId =
        context.getString(R.string.stream_video_incoming_call_low_priority_notification_channel_id)
    private val ringingIncomingLowImportanceChannelId =
        context.getString(
            R.string.stream_video_incoming_call_ringing_low_priority_notification_channel_id,
        )

    fun resolve(
        ringtoneOwner: IncomingRingtoneOwner,
        existingChannelId: String?,
        useLowImportanceChannel: Boolean,
    ): StreamNotificationChannelInfo {
        if (existingChannelId != null) {
            val existingChannel = if (isLowImportanceChannel(existingChannelId)) {
                notificationChannels.incomingCallLowImportanceChannel
            } else {
                notificationChannels.incomingCallChannel
            }
            return existingChannel.withId(existingChannelId)
        }

        val configuredChannel = if (useLowImportanceChannel) {
            notificationChannels.incomingCallLowImportanceChannel
        } else {
            notificationChannels.incomingCallChannel
        }
        return configuredChannel.forRingtoneOwner(ringtoneOwner, useLowImportanceChannel)
    }

    private fun isLowImportanceChannel(channelId: String): Boolean =
        channelId == notificationChannels.incomingCallLowImportanceChannel.id ||
            channelId == legacyIncomingLowImportanceChannelId ||
            channelId == ringingIncomingLowImportanceChannelId

    private fun StreamNotificationChannelInfo.forRingtoneOwner(
        ringtoneOwner: IncomingRingtoneOwner,
        isLowImportance: Boolean,
    ): StreamNotificationChannelInfo {
        val knownDefaultIds = if (isLowImportance) {
            setOf(legacyIncomingLowImportanceChannelId, ringingIncomingLowImportanceChannelId)
        } else {
            setOf(legacyIncomingChannelId, ringingIncomingChannelId)
        }
        if (id !in knownDefaultIds) return this

        val notificationOwnsRingtone =
            isAndroid17OrHigher(sdkInt) && ringtoneOwner == IncomingRingtoneOwner.Notification
        val resolvedId = when {
            notificationOwnsRingtone && isLowImportance -> ringingIncomingLowImportanceChannelId
            notificationOwnsRingtone -> ringingIncomingChannelId
            isLowImportance -> legacyIncomingLowImportanceChannelId
            else -> legacyIncomingChannelId
        }
        return withId(resolvedId)
    }

    private fun StreamNotificationChannelInfo.withId(channelId: String): StreamNotificationChannelInfo =
        if (id == channelId) this else copy(id = channelId)
}
