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
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import io.getstream.video.android.core.R
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

class StreamNotificationChannelsTest {

    @MockK
    lateinit var mockApplication: Application

    @MockK
    lateinit var mockNotificationManager: NotificationManagerCompat

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        // Mock the getString calls for testing
        every { mockApplication.getString(R.string.stream_video_incoming_call_notification_channel_id) } returns "incoming_calls"
        every { mockApplication.getString(R.string.stream_video_incoming_call_notification_channel_title) } returns "Incoming Calls"
        every { mockApplication.getString(R.string.stream_video_incoming_call_notification_channel_description) } returns "Incoming audio and video call alerts"

        every { mockApplication.getString(R.string.stream_video_ongoing_call_notification_channel_id) } returns "ongoing_calls"
        every { mockApplication.getString(R.string.stream_video_ongoing_call_notification_channel_title) } returns "Ongoing Calls"
        every { mockApplication.getString(R.string.stream_video_ongoing_call_notification_channel_description) } returns "Ongoing call notifications"

        every { mockApplication.getString(R.string.stream_video_outgoing_call_notification_channel_id) } returns "outgoing_calls"
        every { mockApplication.getString(R.string.stream_video_outgoing_call_notification_channel_title) } returns "Outgoing Calls"
        every { mockApplication.getString(R.string.stream_video_outgoing_call_notification_channel_description) } returns "Outgoing call notifications"

        every { mockApplication.getString(R.string.stream_video_missed_call_notification_channel_id) } returns "stream_missed_call_channel"
        every { mockApplication.getString(R.string.stream_video_missed_call_notification_channel_title) } returns "Missed Calls"
        every { mockApplication.getString(R.string.stream_video_missed_call_notification_channel_description) } returns "Missed call notifications"
    }

    @Test
    fun `StreamNotificationChannelInfo has correct default importance`() {
        // Given
        val channelInfo = StreamNotificationChannelInfo(
            id = "test_id",
            name = "Test Name",
            description = "Test Description"
        )

        // Then
        assertEquals(NotificationManager.IMPORTANCE_HIGH, channelInfo.importance)
    }

    @Test
    fun `StreamNotificationChannelInfo can override importance`() {
        // Given
        val channelInfo = StreamNotificationChannelInfo(
            id = "test_id",
            name = "Test Name",
            description = "Test Description",
            importance = NotificationManager.IMPORTANCE_LOW
        )

        // Then
        assertEquals(NotificationManager.IMPORTANCE_LOW, channelInfo.importance)
    }

    @Test
    fun `createChannelInfo creates correct channel info`() {
        // When
        val channelInfo = createChannelInfo(
            id = "test_channel",
            name = "Test Channel",
            description = "Test Description",
            importance = NotificationManager.IMPORTANCE_DEFAULT
        )

        // Then
        assertEquals("test_channel", channelInfo.id)
        assertEquals("Test Channel", channelInfo.name)
        assertEquals("Test Description", channelInfo.description)
        assertEquals(NotificationManager.IMPORTANCE_DEFAULT, channelInfo.importance)
    }

    @Test
    fun `createChannelInfoFromResIds creates correct channel info from resources`() {
        // When
        val channelInfo = createChannelInfoFromResIds(
            context = mockApplication,
            idRes = R.string.stream_video_incoming_call_notification_channel_id,
            nameRes = R.string.stream_video_incoming_call_notification_channel_title,
            descriptionRes = R.string.stream_video_incoming_call_notification_channel_description,
            importance = NotificationManager.IMPORTANCE_HIGH
        )

        // Then
        assertEquals("incoming_calls", channelInfo.id)
        assertEquals("Incoming Calls", channelInfo.name)
        assertEquals("Incoming audio and video call alerts", channelInfo.description)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, channelInfo.importance)
    }

    @Test
    fun `createChannelInfoFromResIds uses default importance when not specified`() {
        // When
        val channelInfo = createChannelInfoFromResIds(
            context = mockApplication,
            idRes = R.string.stream_video_incoming_call_notification_channel_id,
            nameRes = R.string.stream_video_incoming_call_notification_channel_title,
            descriptionRes = R.string.stream_video_incoming_call_notification_channel_description
        )

        // Then
        assertEquals(NotificationManager.IMPORTANCE_HIGH, channelInfo.importance)
    }

    @Test
    fun `default incoming call channel has correct values`() {
        // When
        val channelInfo = createChannelInfoFromResIds(
            context = mockApplication,
            idRes = R.string.stream_video_incoming_call_notification_channel_id,
            nameRes = R.string.stream_video_incoming_call_notification_channel_title,
            descriptionRes = R.string.stream_video_incoming_call_notification_channel_description,
            importance = NotificationManager.IMPORTANCE_HIGH
        )

        // Then
        assertEquals("incoming_calls", channelInfo.id)
        assertEquals("Incoming Calls", channelInfo.name)
        assertEquals("Incoming audio and video call alerts", channelInfo.description)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, channelInfo.importance)
    }

    @Test
    fun `default ongoing call channel has correct values`() {
        // When
        val channelInfo = createChannelInfoFromResIds(
            context = mockApplication,
            idRes = R.string.stream_video_ongoing_call_notification_channel_id,
            nameRes = R.string.stream_video_ongoing_call_notification_channel_title,
            descriptionRes = R.string.stream_video_ongoing_call_notification_channel_description,
            importance = NotificationManager.IMPORTANCE_DEFAULT
        )

        // Then
        assertEquals("ongoing_calls", channelInfo.id)
        assertEquals("Ongoing Calls", channelInfo.name)
        assertEquals("Ongoing call notifications", channelInfo.description)
        assertEquals(NotificationManager.IMPORTANCE_DEFAULT, channelInfo.importance)
    }

    @Test
    fun `default outgoing call channel has correct values`() {
        // When
        val channelInfo = createChannelInfoFromResIds(
            context = mockApplication,
            idRes = R.string.stream_video_outgoing_call_notification_channel_id,
            nameRes = R.string.stream_video_outgoing_call_notification_channel_title,
            descriptionRes = R.string.stream_video_outgoing_call_notification_channel_description,
            importance = NotificationManager.IMPORTANCE_DEFAULT
        )

        // Then
        assertEquals("outgoing_calls", channelInfo.id)
        assertEquals("Outgoing Calls", channelInfo.name)
        assertEquals("Outgoing call notifications", channelInfo.description)
        assertEquals(NotificationManager.IMPORTANCE_DEFAULT, channelInfo.importance)
    }

    @Test
    fun `default missed call channel has correct values`() {
        // When
        val channelInfo = createChannelInfoFromResIds(
            context = mockApplication,
            idRes = R.string.stream_video_missed_call_notification_channel_id,
            nameRes = R.string.stream_video_missed_call_notification_channel_title,
            descriptionRes = R.string.stream_video_missed_call_notification_channel_description,
            importance = NotificationManager.IMPORTANCE_HIGH
        )

        // Then
        assertEquals("stream_missed_call_channel", channelInfo.id)
        assertEquals("Missed Calls", channelInfo.name)
        assertEquals("Missed call notifications", channelInfo.description)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, channelInfo.importance)
    }

    @Test
    fun `StreamNotificationChannels contains all required channels`() {
        // Given
        val incomingChannel = createChannelInfo("incoming", "Incoming", "Incoming desc", NotificationManager.IMPORTANCE_HIGH)
        val ongoingChannel = createChannelInfo("ongoing", "Ongoing", "Ongoing desc", NotificationManager.IMPORTANCE_DEFAULT)
        val outgoingChannel = createChannelInfo("outgoing", "Outgoing", "Outgoing desc", NotificationManager.IMPORTANCE_DEFAULT)
        val missedChannel = createChannelInfo("missed", "Missed", "Missed desc", NotificationManager.IMPORTANCE_HIGH)

        // When
        val channels = StreamNotificationChannels(
            incomingCallChannel = incomingChannel,
            ongoingCallChannel = ongoingChannel,
            outgoingCallChannel = outgoingChannel,
            missedCallChannel = missedChannel
        )

        // Then
        assertEquals(incomingChannel, channels.incomingCallChannel)
        assertEquals(ongoingChannel, channels.ongoingCallChannel)
        assertEquals(outgoingChannel, channels.outgoingCallChannel)
        assertEquals(missedChannel, channels.missedCallChannel)
    }

    @Test
    fun `channel creation calls NotificationManagerCompat correctly`() {
        // Given
        val channelInfo = StreamNotificationChannelInfo(
            id = "test_channel",
            name = "Test Channel",
            description = "Test Description",
            importance = NotificationManager.IMPORTANCE_HIGH
        )

        // When
        channelInfo.create(mockNotificationManager)

        // Then
        verify { mockNotificationManager.createNotificationChannel(any<NotificationChannelCompat>()) }
    }
}
