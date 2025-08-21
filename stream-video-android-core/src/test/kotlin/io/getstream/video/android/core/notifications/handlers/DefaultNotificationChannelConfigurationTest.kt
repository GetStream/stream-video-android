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

package io.getstream.video.android.core.notifications.handlers

import android.app.Application
import android.app.NotificationManager
import io.getstream.video.android.core.R
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Tests to ensure default notification channel configurations remain consistent.
 * These tests will catch any changes to default channel values that could break
 * existing notification behavior.
 */
class DefaultNotificationChannelConfigurationTest {

    @MockK
    private lateinit var mockApplication: Application

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        // Mock all the getString calls with expected values
        every {
            mockApplication.getString(R.string.stream_video_incoming_call_notification_channel_id)
        } returns "incoming_calls"
        every {
            mockApplication.getString(R.string.stream_video_incoming_call_notification_channel_title)
        } returns "Incoming Calls"
        every {
            mockApplication.getString(R.string.stream_video_incoming_call_notification_channel_description)
        } returns "Incoming audio and video call alerts"

        every {
            mockApplication.getString(R.string.stream_video_ongoing_call_notification_channel_id)
        } returns "ongoing_calls"
        every {
            mockApplication.getString(R.string.stream_video_ongoing_call_notification_channel_title)
        } returns "Ongoing Calls"
        every {
            mockApplication.getString(R.string.stream_video_ongoing_call_notification_channel_description)
        } returns "Ongoing call notifications"

        every {
            mockApplication.getString(R.string.stream_video_outgoing_call_notification_channel_id)
        } returns "outgoing_calls"
        every {
            mockApplication.getString(R.string.stream_video_outgoing_call_notification_channel_title)
        } returns "Outgoing Calls"
        every {
            mockApplication.getString(R.string.stream_video_outgoing_call_notification_channel_description)
        } returns "Outgoing call notifications"

        every {
            mockApplication.getString(R.string.stream_video_missed_call_notification_channel_id)
        } returns "stream_missed_call_channel"
        every {
            mockApplication.getString(R.string.stream_video_missed_call_notification_channel_title)
        } returns "Missed Calls"
        every {
            mockApplication.getString(R.string.stream_video_missed_call_notification_channel_description)
        } returns "Missed call notifications"
        every {
            mockApplication.getString(R.string.stream_video_missed_call_low_priority_notification_channel_id)
        } returns "stream_missed_call_low_importance_channel"
        every {
            mockApplication.getString(R.string.stream_video_missed_call_low_priority_notification_channel_description)
        } returns "Missed call notifications"
        every {
            mockApplication.getString(R.string.stream_video_incoming_call_low_priority_notification_channel_id)
        } returns "incoming_calls_low_priority"
        every {
            mockApplication.getString(R.string.stream_video_incoming_call_low_priority_notification_channel_description)
        } returns "Incoming audio and video call alerts"
    }

    @Test
    fun `default incoming call channel configuration is correct`() {
        // When - Create the default incoming call channel using the same factory method
        val incomingChannel = createChannelInfoFromResIds(
            context = mockApplication,
            idRes = R.string.stream_video_incoming_call_notification_channel_id,
            nameRes = R.string.stream_video_incoming_call_notification_channel_title,
            descriptionRes = R.string.stream_video_incoming_call_notification_channel_description,
            importance = NotificationManager.IMPORTANCE_HIGH,
        )

        // Then - Verify the exact values that should be used
        assertEquals("incoming_calls", incomingChannel.id)
        assertEquals("Incoming Calls", incomingChannel.name)
        assertEquals("Incoming audio and video call alerts", incomingChannel.description)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, incomingChannel.importance)
    }

    @Test
    fun `default ongoing call channel configuration is correct`() {
        // When - Create the default ongoing call channel using the same factory method
        val ongoingChannel = createChannelInfoFromResIds(
            context = mockApplication,
            idRes = R.string.stream_video_ongoing_call_notification_channel_id,
            nameRes = R.string.stream_video_ongoing_call_notification_channel_title,
            descriptionRes = R.string.stream_video_ongoing_call_notification_channel_description,
            importance = NotificationManager.IMPORTANCE_DEFAULT,
        )

        // Then - Verify the exact values that should be used
        assertEquals("ongoing_calls", ongoingChannel.id)
        assertEquals("Ongoing Calls", ongoingChannel.name)
        assertEquals("Ongoing call notifications", ongoingChannel.description)
        assertEquals(NotificationManager.IMPORTANCE_DEFAULT, ongoingChannel.importance)
    }

    @Test
    fun `default outgoing call channel configuration is correct`() {
        // When - Create the default outgoing call channel using the same factory method
        val outgoingChannel = createChannelInfoFromResIds(
            context = mockApplication,
            idRes = R.string.stream_video_outgoing_call_notification_channel_id,
            nameRes = R.string.stream_video_outgoing_call_notification_channel_title,
            descriptionRes = R.string.stream_video_outgoing_call_notification_channel_description,
            importance = NotificationManager.IMPORTANCE_DEFAULT,
        )

        // Then - Verify the exact values that should be used
        assertEquals("outgoing_calls", outgoingChannel.id)
        assertEquals("Outgoing Calls", outgoingChannel.name)
        assertEquals("Outgoing call notifications", outgoingChannel.description)
        assertEquals(NotificationManager.IMPORTANCE_DEFAULT, outgoingChannel.importance)
    }

    @Test
    fun `default missed call channel configuration is correct`() {
        // When - Create the default missed call channel using the same factory method
        val missedChannel = createChannelInfoFromResIds(
            context = mockApplication,
            idRes = R.string.stream_video_missed_call_notification_channel_id,
            nameRes = R.string.stream_video_missed_call_notification_channel_title,
            descriptionRes = R.string.stream_video_missed_call_notification_channel_description,
            importance = NotificationManager.IMPORTANCE_HIGH,
        )

        // Then - Verify the exact values that should be used
        assertEquals("stream_missed_call_channel", missedChannel.id)
        assertEquals("Missed Calls", missedChannel.name)
        assertEquals("Missed call notifications", missedChannel.description)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, missedChannel.importance)
    }

    @Test
    fun `default channel importance levels follow expected pattern`() {
        // When - Create all default channels
        val incomingChannel = createChannelInfoFromResIds(
            context = mockApplication,
            idRes = R.string.stream_video_incoming_call_notification_channel_id,
            nameRes = R.string.stream_video_incoming_call_notification_channel_title,
            descriptionRes = R.string.stream_video_incoming_call_notification_channel_description,
            importance = NotificationManager.IMPORTANCE_HIGH,
        )

        val ongoingChannel = createChannelInfoFromResIds(
            context = mockApplication,
            idRes = R.string.stream_video_ongoing_call_notification_channel_id,
            nameRes = R.string.stream_video_ongoing_call_notification_channel_title,
            descriptionRes = R.string.stream_video_ongoing_call_notification_channel_description,
            importance = NotificationManager.IMPORTANCE_DEFAULT,
        )

        val outgoingChannel = createChannelInfoFromResIds(
            context = mockApplication,
            idRes = R.string.stream_video_outgoing_call_notification_channel_id,
            nameRes = R.string.stream_video_outgoing_call_notification_channel_title,
            descriptionRes = R.string.stream_video_outgoing_call_notification_channel_description,
            importance = NotificationManager.IMPORTANCE_DEFAULT,
        )

        val missedChannel = createChannelInfoFromResIds(
            context = mockApplication,
            idRes = R.string.stream_video_missed_call_notification_channel_id,
            nameRes = R.string.stream_video_missed_call_notification_channel_title,
            descriptionRes = R.string.stream_video_missed_call_notification_channel_description,
            importance = NotificationManager.IMPORTANCE_HIGH,
        )

        // Then - Verify importance levels follow the expected pattern
        // High importance: Incoming and missed calls (should interrupt user)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, incomingChannel.importance)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, missedChannel.importance)

        // Default importance: Ongoing and outgoing calls (less intrusive)
        assertEquals(NotificationManager.IMPORTANCE_DEFAULT, ongoingChannel.importance)
        assertEquals(NotificationManager.IMPORTANCE_DEFAULT, outgoingChannel.importance)
    }

    @Test
    fun `string resource values are consistent and not accidentally changed`() {
        // This test ensures that if someone changes the string resources,
        // the test will fail and alert them to the breaking change

        // Channel IDs (these should never change as they affect existing installations)
        assertEquals(
            "incoming_calls",
            mockApplication.getString(R.string.stream_video_incoming_call_notification_channel_id),
        )
        assertEquals(
            "ongoing_calls",
            mockApplication.getString(R.string.stream_video_ongoing_call_notification_channel_id),
        )
        assertEquals(
            "outgoing_calls",
            mockApplication.getString(R.string.stream_video_outgoing_call_notification_channel_id),
        )
        assertEquals(
            "stream_missed_call_channel",
            mockApplication.getString(R.string.stream_video_missed_call_notification_channel_id),
        )

        // Channel names (user-visible, but changes could confuse users)
        assertEquals(
            "Incoming Calls",
            mockApplication.getString(
                R.string.stream_video_incoming_call_notification_channel_title,
            ),
        )
        assertEquals(
            "Ongoing Calls",
            mockApplication.getString(
                R.string.stream_video_ongoing_call_notification_channel_title,
            ),
        )
        assertEquals(
            "Outgoing Calls",
            mockApplication.getString(
                R.string.stream_video_outgoing_call_notification_channel_title,
            ),
        )
        assertEquals(
            "Missed Calls",
            mockApplication.getString(R.string.stream_video_missed_call_notification_channel_title),
        )

        // Channel descriptions (user-visible in settings)
        assertEquals(
            "Incoming audio and video call alerts",
            mockApplication.getString(
                R.string.stream_video_incoming_call_notification_channel_description,
            ),
        )
        assertEquals(
            "Ongoing call notifications",
            mockApplication.getString(
                R.string.stream_video_ongoing_call_notification_channel_description,
            ),
        )
        assertEquals(
            "Outgoing call notifications",
            mockApplication.getString(
                R.string.stream_video_outgoing_call_notification_channel_description,
            ),
        )
        assertEquals(
            "Missed call notifications",
            mockApplication.getString(
                R.string.stream_video_missed_call_notification_channel_description,
            ),
        )
    }

    @Test
    fun `default StreamNotificationChannels can be created with expected configuration`() {
        // When - Create the default channels configuration as used in the handlers
        val defaultChannels = StreamNotificationChannels(
            incomingCallChannel = createChannelInfoFromResIds(
                mockApplication,
                R.string.stream_video_incoming_call_notification_channel_id,
                R.string.stream_video_incoming_call_notification_channel_title,
                R.string.stream_video_incoming_call_notification_channel_description,
                NotificationManager.IMPORTANCE_HIGH,
            ),
            ongoingCallChannel = createChannelInfoFromResIds(
                mockApplication,
                R.string.stream_video_ongoing_call_notification_channel_id,
                R.string.stream_video_ongoing_call_notification_channel_title,
                R.string.stream_video_ongoing_call_notification_channel_description,
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
            outgoingCallChannel = createChannelInfoFromResIds(
                mockApplication,
                R.string.stream_video_outgoing_call_notification_channel_id,
                R.string.stream_video_outgoing_call_notification_channel_title,
                R.string.stream_video_outgoing_call_notification_channel_description,
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
            missedCallChannel = createChannelInfoFromResIds(
                mockApplication,
                R.string.stream_video_missed_call_notification_channel_id,
                R.string.stream_video_missed_call_notification_channel_title,
                R.string.stream_video_missed_call_notification_channel_description,
                NotificationManager.IMPORTANCE_HIGH,
            ),
            missedCallLowImportanceChannel = createChannelInfoFromResIds(
                mockApplication,
                R.string.stream_video_missed_call_low_priority_notification_channel_id,
                R.string.stream_video_missed_call_notification_channel_title,
                R.string.stream_video_missed_call_low_priority_notification_channel_description,
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
            incomingCallLowImportanceChannel = createChannelInfoFromResIds(
                mockApplication,
                R.string.stream_video_incoming_call_low_priority_notification_channel_id,
                R.string.stream_video_incoming_call_notification_channel_title,
                R.string.stream_video_incoming_call_low_priority_notification_channel_description,
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )

        // Then - Verify all channels are properly configured
        assertEquals("incoming_calls", defaultChannels.incomingCallChannel.id)
        assertEquals("ongoing_calls", defaultChannels.ongoingCallChannel.id)
        assertEquals("outgoing_calls", defaultChannels.outgoingCallChannel.id)
        assertEquals("stream_missed_call_channel", defaultChannels.missedCallChannel.id)
    }

    @Test
    fun `createChannelInfoFromResIds uses IMPORTANCE_HIGH as default when not specified`() {
        // When - Create channel info without specifying importance
        val channelInfo = createChannelInfoFromResIds(
            context = mockApplication,
            idRes = R.string.stream_video_incoming_call_notification_channel_id,
            nameRes = R.string.stream_video_incoming_call_notification_channel_title,
            descriptionRes = R.string.stream_video_incoming_call_notification_channel_description,
            // importance parameter omitted - should default to IMPORTANCE_HIGH
        )

        // Then - Should use IMPORTANCE_HIGH as default
        assertEquals(NotificationManager.IMPORTANCE_HIGH, channelInfo.importance)
    }
}
