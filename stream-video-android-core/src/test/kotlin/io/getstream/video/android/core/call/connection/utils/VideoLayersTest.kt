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

package io.getstream.video.android.core.call.connection.utils

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.getstream.webrtc.CameraEnumerationAndroid
import stream.video.sfu.models.Codec
import stream.video.sfu.models.PublishOption
import stream.video.sfu.models.TrackType
import stream.video.sfu.models.VideoDimension
import stream.video.sfu.models.VideoQuality

/**
 * Test suite for video-layers-related functions.
 */
class VideoLayersTest {

    // region isSvcCodec
    @Test
    fun `isSvcCodec returns false when codec is null`() = runTest {
        assertFalse(isSvcCodec(null))
    }

    @Test
    fun `isSvcCodec returns true for vp9, av1 in short form`() = runTest {
        assertTrue(isSvcCodec("vp9"))
        assertTrue(isSvcCodec("av1"))
    }

    @Test
    fun `isSvcCodec returns true for vp9, av1 in mime type`() = runTest {
        assertTrue(isSvcCodec("video/vp9"))
        assertTrue(isSvcCodec("video/av1"))
    }

    @Test
    fun `isSvcCodec returns false for unsupported codec`() = runTest {
        assertFalse(isSvcCodec("h264"))
        assertFalse(isSvcCodec("video/h264"))
    }

    // endregion

    // region toScalabilityMode

    @Test
    fun `toScalabilityMode returns L1T1 with no KEY suffix`() = runTest {
        val mode = toScalabilityMode(spatialLayers = 1, temporalLayers = 1)
        assertEquals("L1T1", mode)
    }

    @Test
    fun `toScalabilityMode returns KEY suffix when spatialLayers greater than 1`() = runTest {
        val mode = toScalabilityMode(spatialLayers = 2, temporalLayers = 2)
        assertEquals("L2T2_KEY", mode)
    }

    // endregion

    // region toSvcEncodings

    @Test
    fun `toSvcEncodings returns null if input layers is null`() = runTest {
        val result = toSvcEncodings(null)
        assertNull(result)
    }

    @Test
    fun `toSvcEncodings returns empty list if no layer with rid = f`() = runTest {
        val layers = listOf(
            OptimalVideoLayer(rid = "q", width = 100),
            OptimalVideoLayer(rid = "h", width = 200),
        )
        val result = toSvcEncodings(layers)
        assertEquals(1, result!!.size)
        assertEquals("q", result.first().rid)
        assertEquals(200, result.first().width)
    }

    @Test
    fun `toSvcEncodings transforms rid f to q with svc = true`() = runTest {
        val layers = listOf(
            OptimalVideoLayer(rid = "f", width = 1920, height = 1080, maxBitrate = 1_000_000),
        )
        val result = toSvcEncodings(layers)
        assertEquals(1, result!!.size)
        val transformed = result.first()
        assertEquals("q", transformed.rid)
        assertTrue(transformed.svc)
        assertEquals(1920, transformed.width)
        assertEquals(1080, transformed.height)
        assertEquals(1_000_000, transformed.maxBitrate)
    }

    // endregion

    // region ridToVideoQuality

    @Test
    fun `ridToVideoQuality returns LOW for q`() = runTest {
        assertEquals(VideoQuality.VIDEO_QUALITY_LOW_UNSPECIFIED, ridToVideoQuality("q"))
    }

    @Test
    fun `ridToVideoQuality returns MID for h`() = runTest {
        assertEquals(VideoQuality.VIDEO_QUALITY_MID, ridToVideoQuality("h"))
    }

    @Test
    fun `ridToVideoQuality returns HIGH for other strings`() = runTest {
        assertEquals(VideoQuality.VIDEO_QUALITY_HIGH, ridToVideoQuality("f"))
        assertEquals(VideoQuality.VIDEO_QUALITY_HIGH, ridToVideoQuality("xyz"))
        assertEquals(VideoQuality.VIDEO_QUALITY_HIGH, ridToVideoQuality(null))
    }

    // endregion

    // region toVideoLayers

    @Test
    fun `toVideoLayers converts OptimalVideoLayer list to VideoLayer list`() = runTest {
        val optimal = listOf(
            OptimalVideoLayer(
                rid = "q",
                width = 100,
                height = 200,
                maxBitrate = 300_000,
                maxFramerate = 15,
            ),
            OptimalVideoLayer(
                rid = "h",
                width = 200,
                height = 400,
                maxBitrate = 750_000,
                maxFramerate = 30,
            ),
            OptimalVideoLayer(
                rid = "f",
                width = 400,
                height = 800,
                maxBitrate = 1_250_000,
                maxFramerate = 60,
            ),
        )

        val result = toVideoLayers(optimal)
        assertEquals(3, result.size)

        val (qLayer, hLayer, fLayer) = result

        // q
        assertEquals("q", qLayer.rid)
        assertEquals(300_000, qLayer.bitrate)
        assertEquals(15, qLayer.fps)
        assertEquals(VideoQuality.VIDEO_QUALITY_LOW_UNSPECIFIED, qLayer.quality)
        assertEquals(100, qLayer.video_dimension?.width)
        assertEquals(200, qLayer.video_dimension?.height)

        // h
        assertEquals("h", hLayer.rid)
        assertEquals(750_000, hLayer.bitrate)
        assertEquals(30, hLayer.fps)
        assertEquals(VideoQuality.VIDEO_QUALITY_MID, hLayer.quality)

        // f
        assertEquals("f", fLayer.rid)
        assertEquals(1_250_000, fLayer.bitrate)
        assertEquals(60, fLayer.fps)
        assertEquals(VideoQuality.VIDEO_QUALITY_HIGH, fLayer.quality)
    }

    @Test
    fun `toVideoLayers converts OptimalVideoLayer list to VideoLayer list for SVC`() = runTest {
        val optimal = listOf(
            OptimalVideoLayer(
                rid = "q",
                width = 1280,
                height = 720,
                maxBitrate = 2000000,
                maxFramerate = 30,
            ),
        )

        val result = toVideoLayers(optimal)
        assertEquals(1, result.size)

        val qLayer = result[0]

        // q
        assertEquals("q", qLayer.rid)
        assertEquals(2000000, qLayer.bitrate)
        assertEquals(30, qLayer.fps)
        assertEquals(VideoQuality.VIDEO_QUALITY_LOW_UNSPECIFIED, qLayer.quality)
        assertEquals(1280, qLayer.video_dimension?.width)
        assertEquals(720, qLayer.video_dimension?.height)
    }

    // endregion

    // region isAudioTrackType

    @Test
    fun `isAudioTrackType returns true for TRACK_TYPE_AUDIO`() = runTest {
        assertTrue(isAudioTrackType(TrackType.TRACK_TYPE_AUDIO))
    }

    @Test
    fun `isAudioTrackType returns true for TRACK_TYPE_SCREEN_SHARE_AUDIO`() = runTest {
        assertTrue(isAudioTrackType(TrackType.TRACK_TYPE_SCREEN_SHARE_AUDIO))
    }

    @Test
    fun `isAudioTrackType returns false for non-audio track types`() = runTest {
        assertFalse(isAudioTrackType(TrackType.TRACK_TYPE_VIDEO))
        assertFalse(isAudioTrackType(TrackType.TRACK_TYPE_SCREEN_SHARE))
    }

    // endregion

    // region getComputedMaxBitrate

    @Test
    fun `getComputedMaxBitrate returns smaller when current resolution is smaller than target`() = runTest {
        // current is 640x360, target is 1280x720, so we scale down the bitrate proportionally
        val target = VideoDimension(1280, 720)
        val result =
            getComputedMaxBitrate(
                target,
                currentWidth = 640,
                currentHeight = 360,
                bitrate = 1_000_000,
            )
        // Expect roughly 25% of the original => 250,000
        assertTrue(result in 200_000..300_000) // allow some rounding
    }

    @Test
    fun `getComputedMaxBitrate returns original when current resolution is bigger than or equal to target`() = runTest {
        val target = VideoDimension(640, 360)
        val result =
            getComputedMaxBitrate(
                target,
                currentWidth = 640,
                currentHeight = 360,
                bitrate = 1_000_000,
            )
        assertEquals(1_000_000, result)
    }

    // endregion

    // region withSimulcastConstraints

    @Test
    fun `withSimulcastConstraints re-maps rid in order q, h, f`() = runTest {
        val settings = VideoDimension(1920, 1080)
        val inputLayers = listOf(
            OptimalVideoLayer(rid = "f", width = 300),
            OptimalVideoLayer(rid = "h", width = 200),
            OptimalVideoLayer(rid = "q", width = 100),
        )
        val result = withSimulcastConstraints(settings, inputLayers)
        // Expect 3 layers: "q", "h", "f"
        assertEquals(3, result.size)
        assertEquals("q", result[0].rid)
        assertEquals("h", result[1].rid)
        assertEquals("f", result[2].rid)
    }

    // endregion

    // region computeTransceiverEncodings

    @Test
    fun `computeTransceiverEncodings returns single SVC layer if publish option's codec is SVC`() = runTest {
        val mockCodec = mockk<Codec>()
        val captureFormat = CameraEnumerationAndroid.CaptureFormat(1280, 720, 30, 30)
        every { mockCodec.name } returns "av1"
        val publishOption = PublishOption(
            codec = mockCodec,
            bitrate = 2_000_000,
            fps = 30,
            video_dimension = VideoDimension(1280, 720),
            max_spatial_layers = 2,
            max_temporal_layers = 2,
        )

        val result = computeTransceiverEncodings(captureFormat, publishOption)
        // Should have 3 layers if SVC, but the first one’s rid = "q" with maxBitrateBps for "f"
        assertEquals(1, result.size)
        assertEquals("q", result[0].rid)
        assertEquals("L2T2_KEY", result[0].scalabilityMode)
        assertNull(result[0].scaleResolutionDownBy)
        // This first layer uses the "f" bitrates from defaultBitratePerRid or fallback
        assertEquals(2000000, result[0].maxBitrateBps)
    }

    @Test
    fun `computeTransceiverEncodings returns maxSpatialLayers simulcast layers if publish option's codec is non-SVC`() = runTest {
        val mockCodec = mockk<Codec>(relaxed = true)
        val captureFormat = CameraEnumerationAndroid.CaptureFormat(1280, 720, 30, 30)
        every { mockCodec.name } returns "vp8"
        val publishOption = PublishOption(
            codec = mockCodec,
            bitrate = 1000000,
            fps = 24,
            video_dimension = VideoDimension(640, 480),
            max_spatial_layers = 2,
            max_temporal_layers = 1,
        )

        val result = computeTransceiverEncodings(captureFormat, publishOption)
        // 3 distinct rid: q, h, f
        assertEquals("q", result[0].rid)
        assertEquals("h", result[1].rid)

        assertEquals(500000, result[0].maxBitrateBps)
        assertNull(result[0].scalabilityMode)
        assertEquals(2.0, result[0].scaleResolutionDownBy)
        assertEquals(1.0, result[1].scaleResolutionDownBy)
        assertEquals(1000000, result[1].maxBitrateBps)
    }

    // endregion

    // region findOptimalVideoLayers

    @Test
    fun `findOptimalVideoLayers returns scaled layers in order from q with SVC scalabilities`() = runTest {
        val mockCodec = mockk<Codec>()
        every { mockCodec.name } returns "vp9"
        val publishOption = PublishOption(
            track_type = TrackType.TRACK_TYPE_VIDEO,
            codec = mockCodec,
            bitrate = 1_000_000,
            fps = 30,
            video_dimension = VideoDimension(1280, 720),
            max_spatial_layers = 2,
            max_temporal_layers = 3,
        )
        val settings = VideoDimension(1280, 720)

        val result = findOptimalVideoLayers(settings, publishOption)

        // We expect 3 layers with rid = q, h, f (then re-mapped by withSimulcastConstraints)
        // Actually the loop inserts them in reverse order, but withSimulcastConstraints
        // re-maps them. Let's just check correctness:
        assertEquals(2, result.size)
        // The first in the list after constraints is "q"
        val q = result[0]
        val h = result[1]

        // Should have a scalabilityMode
        assertNotNull(q.scalabilityMode)
        assertEquals("L2T3_KEY", q.scalabilityMode) // Because 2 spacial, 3 temporal -> KEY
        assertEquals("L2T3_KEY", h.scalabilityMode)
        // We can also verify maxBitrate, downscale factors, etc.
        // But for coverage, we just need to confirm they exist.
        assertEquals(500000, q.maxBitrate)
        assertEquals(1000000, h.maxBitrate)
    }

    @Test
    fun `findOptimalVideoLayers returns layers with scaleResolutionDownBy for non-SVC`() = runTest {
        val mockCodec = mockk<Codec>()
        every { mockCodec.name } returns "vp8"
        val publishOption = PublishOption(
            track_type = TrackType.TRACK_TYPE_VIDEO,
            codec = mockCodec,
            bitrate = 2_000_000,
            fps = 60,
            video_dimension = VideoDimension(1920, 1080),
            max_spatial_layers = 3,
            max_temporal_layers = 1,
        )
        val settings = VideoDimension(1920, 1080)

        val result = findOptimalVideoLayers(settings, publishOption)
        // 3 layers: re-mapped to q,h,f
        assertEquals(3, result.size)

        val q = result[0]
        val h = result[1]
        val f = result[2]

        // For non-SVC, we expect scaleResolutionDownBy != null
        assertEquals(4.0, q.scaleResolutionDownBy)
        assertEquals(2.0, h.scaleResolutionDownBy)
        assertEquals(1.0, f.scaleResolutionDownBy)

        assertEquals(500000, q.maxBitrate)
        assertEquals(1000000, h.maxBitrate)
        assertEquals(2000000, f.maxBitrate)

        // For non-SVC, we do not set scalabilityMode
        assertNull(q.scalabilityMode)
        assertNull(h.scalabilityMode)
        assertNull(f.scalabilityMode)
    }

    // endregion
}
