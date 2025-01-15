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

package io.getstream.video.android.core.call.connection.transceivers

import io.getstream.video.android.core.call.connection.utils.OptimalVideoLayer
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.webrtc.RtpReceiver
import org.webrtc.RtpSender
import org.webrtc.RtpTransceiver
import stream.video.sfu.models.PublishOption
import stream.video.sfu.models.TrackType
import stream.video.sfu.models.VideoDimension

class TransceiverCacheTest {

    @MockK
    lateinit var mockTransceiver1: RtpTransceiver

    @MockK
    lateinit var mockTransceiver2: RtpTransceiver

    @MockK
    lateinit var mockSender1: RtpSender

    @MockK
    lateinit var mockSender2: RtpSender

    @MockK
    lateinit var mockReceiver1: RtpReceiver

    @MockK
    lateinit var mockReceiver2: RtpReceiver

    private lateinit var cache: TransceiverCache

    // Sample PublishOptions
    private val publishOption1 = PublishOption(
        id = 1,
        track_type = TrackType.TRACK_TYPE_VIDEO,
        bitrate = 1_000_000,
        fps = 30,
        video_dimension = VideoDimension(1280, 720),
        max_spatial_layers = 1,
        max_temporal_layers = 1,
        codec = null, // or mock
    )

    private val publishOption2 = PublishOption(
        id = 2,
        track_type = TrackType.TRACK_TYPE_AUDIO,
        bitrate = 500_000,
        fps = 15,
        video_dimension = null,
        max_spatial_layers = 1,
        max_temporal_layers = 1,
        codec = null,
    )

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        cache = TransceiverCache()

        // Mock the senders/receivers to return a track that is not disposed by default
        every { mockTransceiver1.sender } returns mockSender1
        every { mockTransceiver2.sender } returns mockSender2
        every { mockTransceiver1.receiver } returns mockReceiver1
        every { mockTransceiver2.receiver } returns mockReceiver2

        // By default, let's pretend the track is not disposed
        every { mockSender1.track()?.isDisposed } returns false
        every { mockSender2.track()?.isDisposed } returns false
    }

    @Test
    fun `test add and get`() = runTest {
        cache.add(publishOption1, mockTransceiver1)
        val retrieved = cache.get(publishOption1)
        assertNotNull(retrieved)
        assertEquals(mockTransceiver1, retrieved)
    }

    @Test
    fun `test remove`() = runTest {
        // Add, then remove
        cache.add(publishOption1, mockTransceiver1)
        cache.remove(publishOption1)
        val retrieved = cache.get(publishOption1)
        assertNull(retrieved)
    }

    @Test
    fun `test has`() = runTest {
        cache.add(publishOption1, mockTransceiver1)
        assertTrue(cache.has(publishOption1))
        // Non-existent publishOption
        assertFalse(cache.has(publishOption2))
    }

    @Test
    fun `test items excludes disposed tracks`() = runTest {
        // By default, not disposed
        cache.add(publishOption1, mockTransceiver1)
        cache.add(publishOption2, mockTransceiver2)

        var items = cache.items()
        assertEquals(2, items.size)

        // Now mock one track to be disposed
        every { mockSender2.track()?.isDisposed } returns true

        // We expect it to be filtered out
        items = cache.items()
        assertEquals(1, items.size)
        assertEquals(1, items.first().publishOption.id)
    }

    @Test
    fun `test indexOf`() = runTest {
        cache.add(publishOption1, mockTransceiver1)
        cache.add(publishOption2, mockTransceiver2)

        val index1 = cache.indexOf(publishOption1)
        val index2 = cache.indexOf(publishOption2)
        val indexUnknown = cache.indexOf(
            publishOption1.copy(id = 3), // different key
        )

        assertEquals(0, index1)
        assertEquals(1, index2)
        // Not found => -1
        assertEquals(-1, indexUnknown)
    }

    @Test
    fun `test setLayers and getLayers`() = runTest {
        val layers = listOf(
            OptimalVideoLayer(rid = "f", width = 1920, height = 1080, maxBitrate = 1_000_000),
            OptimalVideoLayer(rid = "h", width = 1280, height = 720, maxBitrate = 500_000),
        )
        cache.setLayers(publishOption1, layers)
        val retrievedLayers = cache.getLayers(publishOption1)
        assertNotNull(retrievedLayers)
        assertEquals(2, retrievedLayers?.size)
        assertEquals("f", retrievedLayers?.first()?.rid)
    }

    @Test
    fun `test setLayers overwrites existing layers`() = runTest {
        val oldLayers = listOf(
            OptimalVideoLayer(rid = "old", width = 640, height = 360, maxBitrate = 200_000),
        )
        val newLayers = listOf(
            OptimalVideoLayer(rid = "new", width = 1920, height = 1080, maxBitrate = 1_000_000),
        )

        // Set old layers
        cache.setLayers(publishOption1, oldLayers)
        val old = cache.getLayers(publishOption1)
        assertEquals("old", old?.first()?.rid)

        // Overwrite with new
        cache.setLayers(publishOption1, newLayers)
        val updated = cache.getLayers(publishOption1)
        assertEquals("new", updated?.first()?.rid)
        assertEquals(1, updated?.size)
    }

    @Test
    fun `test setLayers with empty default`() = runTest {
        // If we call setLayers with no second arg, it sets an emptyList
        cache.setLayers(publishOption1)
        assertTrue(cache.getLayers(publishOption1)?.isEmpty() == true)
    }
}
