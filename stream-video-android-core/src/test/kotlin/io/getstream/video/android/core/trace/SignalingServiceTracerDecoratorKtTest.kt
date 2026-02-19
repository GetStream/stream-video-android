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

package io.getstream.video.android.core.trace

import io.getstream.video.android.core.api.SignalServerService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import stream.video.sfu.signal.ICERestartResponse
import stream.video.sfu.signal.ICETrickleResponse
import stream.video.sfu.signal.SendAnswerResponse
import stream.video.sfu.signal.SetPublisherResponse
import stream.video.sfu.signal.StartNoiseCancellationResponse
import stream.video.sfu.signal.StopNoiseCancellationResponse
import stream.video.sfu.signal.UpdateMuteStatesResponse
import stream.video.sfu.signal.UpdateSubscriptionsResponse

class SignalingServiceTracerDecoratorKtTest {

    @Test
    fun `tracedWith proxies method calls and traces them`() = runTest {
        val tracer = spyk(Tracer("iface"))
        val impl = mockk<SignalServerService>(relaxed = true)
        val proxy = tracedWith<SignalServerService>(impl, tracer)

        proxy.setPublisher(mockk(relaxed = true))
        proxy.sendAnswer(mockk(relaxed = true))
        proxy.iceTrickle(mockk(relaxed = true))
        proxy.updateSubscriptions(mockk(relaxed = true))
        proxy.updateMuteStates(mockk(relaxed = true))
        proxy.iceRestart(mockk(relaxed = true))
        proxy.startNoiseCancellation(mockk(relaxed = true))
        proxy.stopNoiseCancellation(mockk(relaxed = true))
        proxy.sendStats(mockk(relaxed = true))

        coVerify { tracer.trace("setPublisher", any()) }
        coVerify { tracer.trace("sendAnswer", any()) }
        coVerify { tracer.trace("iceTrickle", any()) }
        coVerify { tracer.trace("updateSubscriptions", any()) }
        coVerify { tracer.trace("updateMuteStates", any()) }
        coVerify { tracer.trace("iceRestart", any()) }
        coVerify { tracer.trace("startNoiseCancellation", any()) }
        coVerify { tracer.trace("stopNoiseCancellation", any()) }
        coVerify(exactly = 0) { tracer.trace("sendStats", any()) }
    }

    @Test
    fun `tracedWith proxies method calls and traces errors`() = runTest {
        val tracer = spyk(Tracer("iface"))
        val impl = mockk<SignalServerService>(relaxed = true) {
            coEvery { setPublisher(any()) } returns SetPublisherResponse(error = mockk(relaxed = true))
            coEvery { sendAnswer(any()) } returns SendAnswerResponse(error = mockk(relaxed = true))
            coEvery { iceTrickle(any()) } returns ICETrickleResponse(error = mockk(relaxed = true))
            coEvery {
                updateSubscriptions(any())
            } returns UpdateSubscriptionsResponse(error = mockk(relaxed = true))
            coEvery { updateMuteStates(any()) } returns UpdateMuteStatesResponse(error = mockk(relaxed = true))
            coEvery { iceRestart(any()) } returns ICERestartResponse(error = mockk(relaxed = true))
            coEvery {
                startNoiseCancellation(any())
            } returns StartNoiseCancellationResponse(error = mockk(relaxed = true))
            coEvery {
                stopNoiseCancellation(any())
            } returns StopNoiseCancellationResponse(error = mockk(relaxed = true))
        }
        val proxy = tracedWith<SignalServerService>(impl, tracer)

        proxy.setPublisher(mockk(relaxed = true))
        proxy.sendAnswer(mockk(relaxed = true))
        proxy.iceTrickle(mockk(relaxed = true))
        proxy.updateSubscriptions(mockk(relaxed = true))
        proxy.updateMuteStates(mockk(relaxed = true))
        proxy.iceRestart(mockk(relaxed = true))
        proxy.startNoiseCancellation(mockk(relaxed = true))
        proxy.stopNoiseCancellation(mockk(relaxed = true))

        coVerify { tracer.trace("setPublisher-error", any()) }
        coVerify { tracer.trace("sendAnswer-error", any()) }
        coVerify { tracer.trace("iceTrickle-error", any()) }
        coVerify { tracer.trace("updateSubscriptions-error", any()) }
        coVerify { tracer.trace("updateMuteStates-error", any()) }
        coVerify { tracer.trace("iceRestart-error", any()) }
        coVerify { tracer.trace("startNoiseCancellation-error", any()) }
        coVerify { tracer.trace("stopNoiseCancellation-error", any()) }
    }
}
