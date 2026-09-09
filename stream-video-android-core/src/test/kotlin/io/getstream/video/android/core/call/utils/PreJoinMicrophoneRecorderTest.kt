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

package io.getstream.video.android.core.call.utils

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioRecord
import androidx.core.content.ContextCompat
import com.google.common.truth.Truth.assertThat
import io.getstream.video.android.core.dispatchers.DispatcherProvider
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Unit tests for [PreJoinMicrophoneRecorder], which reads the microphone while the call has no
 * RTC session. [AudioRecord] is mocked, so no test opens a real microphone.
 */
class PreJoinMicrophoneRecorderTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var context: Context
    private val samples = mutableListOf<ByteArray>()

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        samples.clear()
        mockkStatic(ContextCompat::class)
        mockkStatic(AudioRecord::class)
        mockkConstructor(AudioRecord::class)
        DispatcherProvider.set(testDispatcher, testDispatcher)
        grantRecordAudio(true)
    }

    @After
    fun tearDown() {
        DispatcherProvider.reset()
        unmockkAll()
    }

    private fun recorder() = PreJoinMicrophoneRecorder(context, testScope) { samples.add(it) }

    private fun grantRecordAudio(granted: Boolean) {
        every { ContextCompat.checkSelfPermission(context, any()) } returns
            if (granted) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED
    }

    /** One buffer of [firstRead] bytes, then an error so the read loop ends. */
    private fun givenMicrophone(firstRead: Int) {
        every { AudioRecord.getMinBufferSize(any(), any(), any()) } returns 640
        every { anyConstructed<AudioRecord>().state } returns AudioRecord.STATE_INITIALIZED
        every { anyConstructed<AudioRecord>().startRecording() } just runs
        every { anyConstructed<AudioRecord>().recordingState } returns
            AudioRecord.RECORDSTATE_RECORDING
        every { anyConstructed<AudioRecord>().stop() } just runs
        every { anyConstructed<AudioRecord>().release() } just runs
        var reads = 0
        every { anyConstructed<AudioRecord>().read(any<ByteArray>(), any(), any()) } answers {
            if (reads++ == 0) firstRead else AudioRecord.ERROR_INVALID_OPERATION
        }
    }

    @Test
    fun `samples read from the microphone reach the callback`() = runTest(testDispatcher) {
        givenMicrophone(firstRead = 320)

        recorder().start()
        advanceUntilIdle()

        assertThat(samples).hasSize(1)
        assertThat(samples.single()).hasLength(320)
    }

    @Test
    fun `nothing is recorded without the RECORD_AUDIO permission`() = runTest(testDispatcher) {
        grantRecordAudio(false)
        givenMicrophone(firstRead = 320)

        recorder().start()
        advanceUntilIdle()

        assertThat(samples).isEmpty()
    }

    /**
     * A source can be constructed and still refuse to record, so the fallback has to cover the
     * start as well as the construction.
     */
    @Test
    fun `a source that will not start recording falls back to the next one`() = runTest(
        testDispatcher,
    ) {
        givenMicrophone(firstRead = 320)
        var starts = 0
        every { anyConstructed<AudioRecord>().recordingState } answers {
            if (starts++ == 0) AudioRecord.RECORDSTATE_STOPPED else AudioRecord.RECORDSTATE_RECORDING
        }

        recorder().start()
        advanceUntilIdle()

        assertThat(samples).hasSize(1)
    }

    @Test
    fun `an unusable microphone is given up on instead of reading`() = runTest(testDispatcher) {
        givenMicrophone(firstRead = 320)
        every { AudioRecord.getMinBufferSize(any(), any(), any()) } returns
            AudioRecord.ERROR_BAD_VALUE

        recorder().start()
        advanceUntilIdle()

        assertThat(samples).isEmpty()
    }

    @Test
    fun `stop is safe before anything was started`() = runTest(testDispatcher) {
        recorder().stop()
    }

    /**
     * The session takes the microphone as soon as the recorder lets go, so [
     * PreJoinMicrophoneRecorder.stop] has to return after the device is released rather than
     * after the read loop is merely asked to end.
     *
     * Runs the read loop on a real dispatcher with a blocking read, the way a microphone behaves:
     * on the test dispatcher the loop never yields, so nothing would be in flight to wait for.
     */
    @Test
    fun `stop returns only once the microphone has been released`() = runBlocking {
        DispatcherProvider.set(testDispatcher, Dispatchers.IO)
        val reading = CountDownLatch(1)
        val released = AtomicBoolean(false)
        givenMicrophone(firstRead = 320)
        every { anyConstructed<AudioRecord>().release() } answers { released.set(true) }
        every { anyConstructed<AudioRecord>().read(any<ByteArray>(), any(), any()) } answers {
            reading.countDown()
            Thread.sleep(BLOCKING_READ_MS)
            320
        }
        val scope = CoroutineScope(Dispatchers.IO)
        val recorder = PreJoinMicrophoneRecorder(context, scope) { samples.add(it) }

        recorder.start()
        assertThat(reading.await(5, TimeUnit.SECONDS)).isTrue()
        recorder.stop()

        assertThat(released.get()).isTrue()
        scope.cancel()
    }

    private companion object {
        /** Long enough that a stop which does not wait would return mid-read. */
        const val BLOCKING_READ_MS = 200L
    }
}
