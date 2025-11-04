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

package io.getstream.video.android.core.call.connection.coding

import androidx.core.content.ContextCompat
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.webrtc.SoftwareVideoDecoderFactory
import org.webrtc.VideoCodecInfo
import org.webrtc.VideoDecoder
import org.webrtc.WrappedVideoDecoderFactory

@RunWith(RobolectricTestRunner::class)
class SelectiveVideoDecoderFactoryTest {

    private lateinit var softwareFactory: SoftwareVideoDecoderFactory
    private lateinit var wrappedFactory: WrappedVideoDecoderFactory
    private lateinit var decoderFactory: SelectiveVideoDecoderFactory
    private lateinit var codecInfo: VideoCodecInfo
    private val dummyDecoder = mockk<VideoDecoder>(relaxed = true)

    @Before
    fun setup() {
        mockkConstructor(SoftwareVideoDecoderFactory::class)
        mockkConstructor(WrappedVideoDecoderFactory::class)

        softwareFactory = mockk(relaxed = true)
        wrappedFactory = mockk(relaxed = true)

        // Mock factory creation
        every { anyConstructed<SoftwareVideoDecoderFactory>().createDecoder(any()) } answers {
            softwareFactory.createDecoder(firstArg())
        }
        every { anyConstructed<WrappedVideoDecoderFactory>().createDecoder(any()) } answers {
            wrappedFactory.createDecoder(firstArg())
        }

        every { softwareFactory.createDecoder(any()) } returns dummyDecoder
        every { wrappedFactory.createDecoder(any()) } returns dummyDecoder
        every { softwareFactory.supportedCodecs } returns arrayOf(VideoCodecInfo("SW", mapOf(), listOf()))
        every { wrappedFactory.supportedCodecs } returns arrayOf(VideoCodecInfo("HW", mapOf(), listOf()))

        codecInfo = VideoCodecInfo("VP9", mapOf(), listOf())

        decoderFactory = SelectiveVideoDecoderFactory(
            sharedContext = null,
            forceSWCodec = false,
            forceSWCodecs = listOf("VP9"),
            softwareFactory,
            wrappedFactory,
        )
    }

    @After
    fun tearDown() {
        unmockkStatic(ContextCompat::class)
        clearAllMocks()
    }

    @Test
    fun `createDecoder should use software factory when forceSWCodec is true`() {
        decoderFactory.setForceSWCodec(true)

        decoderFactory.createDecoder(codecInfo)

        verify(exactly = 1) { softwareFactory.createDecoder(codecInfo) }
        verify(exactly = 0) { wrappedFactory.createDecoder(codecInfo) }
    }

    @Test
    fun `createDecoder should use software factory when codec is in forceSWCodecs`() {
        decoderFactory.setForceSWCodec(false)
        decoderFactory.setForceSWCodecList(listOf("VP9"))

        decoderFactory.createDecoder(codecInfo)

        verify(exactly = 1) { softwareFactory.createDecoder(codecInfo) }
        verify(exactly = 0) { wrappedFactory.createDecoder(codecInfo) }
    }

    @Test
    fun `createDecoder should use wrapped factory when codec is not in forceSWCodecs`() {
        decoderFactory.setForceSWCodec(false)
        decoderFactory.setForceSWCodecList(listOf("AV1"))

        decoderFactory.createDecoder(codecInfo)

        verify(exactly = 0) { softwareFactory.createDecoder(codecInfo) }
        verify(exactly = 1) { wrappedFactory.createDecoder(codecInfo) }
    }

    @Test
    fun `getSupportedCodecs should use software factory when forceSWCodec true and list empty`() {
        decoderFactory.setForceSWCodec(true)
        decoderFactory.setForceSWCodecList(emptyList())

        val result = decoderFactory.getSupportedCodecs()

        assert(result[0].name == "SW")
        verify { softwareFactory.supportedCodecs }
    }

    @Test
    fun `getSupportedCodecs should use wrapped factory otherwise`() {
        decoderFactory.setForceSWCodec(false)
        val result = decoderFactory.getSupportedCodecs()

        assert(result[0].name == "HW")
        verify { wrappedFactory.supportedCodecs }
    }
}
