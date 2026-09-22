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
import io.getstream.video.android.core.IncomingRingtoneOwner
import io.getstream.video.android.core.R
import io.getstream.video.android.core.utils.BUILD_VERSION_CODES_CINNAMON_BUN
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class IncomingCallChannelResolverTest {

    @Test
    fun `Android 17 notification owner uses ringing channel`() {
        val channel = channelResolver(BUILD_VERSION_CODES_CINNAMON_BUN).resolve(
            ringtoneOwner = IncomingRingtoneOwner.Notification,
            existingChannelId = null,
            useLowImportanceChannel = false,
        )

        assertEquals(RINGING_CHANNEL_ID, channel.id)
    }

    @Test
    fun `Android 17 legacy owner uses legacy channel`() {
        val channel = channelResolver(BUILD_VERSION_CODES_CINNAMON_BUN).resolve(
            ringtoneOwner = IncomingRingtoneOwner.Legacy,
            existingChannelId = null,
            useLowImportanceChannel = false,
        )

        assertEquals(LEGACY_CHANNEL_ID, channel.id)
    }

    @Test
    fun `pre Android 17 legacy owner uses legacy channel`() {
        val channel = channelResolver(BUILD_VERSION_CODES_CINNAMON_BUN - 1).resolve(
            ringtoneOwner = IncomingRingtoneOwner.Legacy,
            existingChannelId = null,
            useLowImportanceChannel = false,
        )

        assertEquals(LEGACY_CHANNEL_ID, channel.id)
    }

    @Test
    fun `notification update preserves existing channel regardless of foreground state`() {
        val resolver = channelResolver(BUILD_VERSION_CODES_CINNAMON_BUN)

        val backgroundChannel = resolver.resolve(
            ringtoneOwner = IncomingRingtoneOwner.Notification,
            existingChannelId = LEGACY_CHANNEL_ID,
            useLowImportanceChannel = false,
        )
        val foregroundChannel = resolver.resolve(
            ringtoneOwner = IncomingRingtoneOwner.Notification,
            existingChannelId = LEGACY_CHANNEL_ID,
            useLowImportanceChannel = true,
        )

        assertEquals(LEGACY_CHANNEL_ID, backgroundChannel.id)
        assertEquals(LEGACY_CHANNEL_ID, foregroundChannel.id)
    }

    private fun channelResolver(sdkInt: Int): IncomingCallChannelResolver {
        val context = mockk<Context>()
        every {
            context.getString(R.string.stream_video_incoming_call_notification_channel_id)
        } returns LEGACY_CHANNEL_ID
        every {
            context.getString(
                R.string.stream_video_incoming_call_low_priority_notification_channel_id,
            )
        } returns LEGACY_LOW_IMPORTANCE_CHANNEL_ID
        every {
            context.getString(R.string.stream_video_incoming_call_ringing_notification_channel_id)
        } returns RINGING_CHANNEL_ID
        every {
            context.getString(
                R.string.stream_video_incoming_call_ringing_low_priority_notification_channel_id,
            )
        } returns RINGING_LOW_IMPORTANCE_CHANNEL_ID

        val usesAndroid17Defaults = sdkInt >= BUILD_VERSION_CODES_CINNAMON_BUN
        val incomingChannelId =
            if (usesAndroid17Defaults) RINGING_CHANNEL_ID else LEGACY_CHANNEL_ID
        val incomingLowImportanceChannelId = if (usesAndroid17Defaults) {
            RINGING_LOW_IMPORTANCE_CHANNEL_ID
        } else {
            LEGACY_LOW_IMPORTANCE_CHANNEL_ID
        }
        return IncomingCallChannelResolver(
            context = context,
            notificationChannels = StreamNotificationChannels(
                incomingCallChannel = channelInfo(incomingChannelId),
                ongoingCallChannel = channelInfo("ongoing"),
                outgoingCallChannel = channelInfo("outgoing"),
                missedCallChannel = channelInfo("missed"),
                missedCallLowImportanceChannel = channelInfo("missed-low-importance"),
                incomingCallLowImportanceChannel = channelInfo(incomingLowImportanceChannelId),
            ),
            sdkInt = sdkInt,
        )
    }

    private fun channelInfo(id: String) = StreamNotificationChannelInfo(
        id = id,
        name = id,
        description = id,
    )

    private companion object {
        const val LEGACY_CHANNEL_ID = "incoming-calls"
        const val LEGACY_LOW_IMPORTANCE_CHANNEL_ID = "incoming-calls-low-importance"
        const val RINGING_CHANNEL_ID = "incoming-calls-ringing"
        const val RINGING_LOW_IMPORTANCE_CHANNEL_ID = "incoming-calls-ringing-low-importance"
    }
}
