/*
 * Copyright (c) 2014-2025 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core.notifications.internal.service.observers

import android.content.Context
import android.media.AudioManager
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.CallState
import io.getstream.video.android.core.RingingState
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.model.RejectReason
import io.getstream.video.android.core.sounds.CallSoundAndVibrationPlayer
import io.getstream.video.android.core.sounds.MutedRingingConfig
import io.getstream.video.android.core.sounds.RingingCallVibrationConfig
import io.getstream.video.android.core.sounds.RingingConfig
import io.getstream.video.android.core.sounds.Sounds
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class CallServiceRingingStateObserverTest {

    private val dispatcher = StandardTestDispatcher()
    private val testScope = TestScope(dispatcher)

    private lateinit var call: Call
    private lateinit var callState: CallState
    private lateinit var soundPlayer: CallSoundAndVibrationPlayer
    private lateinit var streamVideo: StreamVideoClient
    private lateinit var observer: CallServiceRingingStateObserver

    private val ringingStateFlow =
        MutableStateFlow<RingingState>(RingingState.Idle)

    private val onStopServiceInvoked = mutableListOf<Unit>()

    private val testContext: Context = mockk()
    private val audioManager: AudioManager = mockk()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)

        callState = mockk {
            every { this@mockk.ringingState } returns ringingStateFlow
        }

        call = mockk {
            every { this@mockk.scope } returns testScope
            every { this@mockk.state } returns callState
            coEvery { this@mockk.reject(any(), any()) } returns mockk(relaxed = true)
        }

        soundPlayer = mockk(relaxed = true)

        every { testContext.getSystemService(Context.AUDIO_SERVICE) } returns audioManager
        every { audioManager.ringerMode } returns AudioManager.RINGER_MODE_NORMAL

        val vibrationConfig = mockk<RingingCallVibrationConfig> {
            every { this@mockk.enabled } returns true
            every { this@mockk.vibratePattern } returns longArrayOf(0, 100)
        }

        val ringingConfig = mockk<RingingConfig> {
            every { this@mockk.incomingCallSoundUri } returns mockk()
            every { this@mockk.outgoingCallSoundUri } returns mockk()
        }

        val mutedConfig = mockk<MutedRingingConfig> {
            every { this@mockk.playIncomingSoundIfMuted } returns true
            every { this@mockk.playOutgoingSoundIfMuted } returns true
        }

        val sounds = mockk<Sounds> {
            every { this@mockk.ringingConfig } returns ringingConfig
            every { mutedRingingConfig } returns mutedConfig
        }

        streamVideo = mockk {
            every { this@mockk.context } returns testContext
            every { this@mockk.vibrationConfig } returns vibrationConfig
            every { this@mockk.sounds } returns sounds
        }

        observer = CallServiceRingingStateObserver(
            call = call,
            soundPlayer = soundPlayer,
            streamVideo = streamVideo,
            scope = testScope.backgroundScope,
        )
    }

    @Test
    fun `incoming not accepted plays sound and vibrates`() = runTest {
        observer.observe { onStopServiceInvoked.add(Unit) }
        advanceUntilIdle()

        ringingStateFlow.value = RingingState.Incoming(acceptedByMe = false)
        advanceUntilIdle()
        advanceTimeBy(100L)
        verify {
            soundPlayer.vibrate(any())
            soundPlayer.playCallSound(any(), true)
        }
    }

    @Test
    fun `incoming accepted stops sound`() = runTest {
        observer.observe { }
        advanceUntilIdle()

        ringingStateFlow.value = RingingState.Incoming(acceptedByMe = true)
        advanceUntilIdle()
        advanceTimeBy(100L)
        verify { soundPlayer.stopCallSound() }
    }

    @Test
    fun `outgoing not accepted plays outgoing sound`() = runTest {
        observer.observe { }
        advanceUntilIdle()

        ringingStateFlow.value = RingingState.Outgoing(acceptedByCallee = false)
        advanceUntilIdle()

        verify {
            soundPlayer.playCallSound(any(), true)
        }
    }

    @Test
    fun `outgoing accepted stops sound`() = runTest {
        observer.observe { }
        advanceUntilIdle()

        ringingStateFlow.value = RingingState.Outgoing(acceptedByCallee = true)
        advanceUntilIdle()

        verify { soundPlayer.stopCallSound() }
    }

    @Test
    fun `active call stops sound`() = runTest {
        observer.observe { }
        advanceUntilIdle()

        ringingStateFlow.value = RingingState.Active
        advanceUntilIdle()

        verify { soundPlayer.stopCallSound() }
    }

    @Test
    fun `rejected by all rejects call and stops service`() = runTest {
        observer.observe { onStopServiceInvoked.add(Unit) }
        advanceUntilIdle()

        ringingStateFlow.value = RingingState.RejectedByAll
        advanceUntilIdle()

        coVerify {
            call.reject(
                source = "RingingState.RejectedByAll",
                reason = RejectReason.Decline,
            )
        }

        verify { soundPlayer.stopCallSound() }
        assertEquals(1, onStopServiceInvoked.size)
    }

    @Test
    fun `timeout no answer stops sound`() = runTest {
        observer.observe { }
        advanceUntilIdle()

        ringingStateFlow.value = RingingState.TimeoutNoAnswer
        advanceUntilIdle()
        advanceTimeBy(100L)
        verify { soundPlayer.stopCallSound() }
    }

    @Test
    fun `incoming does not vibrate when vibration disabled`() = runTest {
        every { streamVideo.vibrationConfig.enabled } returns false

        observer.observe { }
        advanceUntilIdle()

        ringingStateFlow.value = RingingState.Incoming(acceptedByMe = false)
        advanceUntilIdle()
        advanceTimeBy(100L)
        verify(exactly = 0) { soundPlayer.vibrate(any()) }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
}
