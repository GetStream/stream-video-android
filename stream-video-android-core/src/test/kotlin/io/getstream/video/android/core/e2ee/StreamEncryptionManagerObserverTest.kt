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

package io.getstream.video.android.core.e2ee

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.webrtc.EncryptionManager

/**
 * Covers how [StreamEncryptionManager] shares WebRTC's single observer slot.
 *
 * The app registers a listener to surface key problems, and the SDK registers one to trace them
 * into call stats. Native only holds one observer, so the manager owns that slot and fans out; the
 * tests below are mostly about neither registration silently displacing the other.
 */
class StreamEncryptionManagerObserverTest {

    private val observerSlot = slot<EncryptionManager.Observer>()
    private var disposed = false

    private val native = mockk<EncryptionManager>(relaxed = true) {
        every { isDisposed } answers { disposed }
        every { setObserver(any()) } answers {
            observerSlot.captured = firstArg()
        }
    }

    /** [StreamEncryptionManager] is only constructible through `create`, which needs native. */
    private fun manager(): StreamEncryptionManager = StreamEncryptionManager::class.java
        .getDeclaredConstructor(EncryptionManager::class.java)
        .apply { isAccessible = true }
        .newInstance(native)

    /**
     * Native events carry final fields and a package-private constructor, so a real one is built
     * reflectively rather than mocked.
     */
    private fun nativeEvent(
        type: EncryptionManager.E2eeEventType = EncryptionManager.E2eeEventType.DECRYPTION_FAILED,
        userId: String = "alice",
    ): EncryptionManager.E2eeEvent = EncryptionManager.E2eeEvent::class.java
        .declaredConstructors
        .single()
        .apply { isAccessible = true }
        .newInstance(type, userId, null, null, null, null, null, null, null)
        as EncryptionManager.E2eeEvent

    private fun emit(event: EncryptionManager.E2eeEvent = nativeEvent()) {
        observerSlot.captured.onE2eeEvent(event)
    }

    @Test
    fun `the app listener receives native events`() {
        val received = mutableListOf<E2EEEvent>()
        manager().setEventListener { received += it }

        emit()

        assertEquals(listOf(E2EEEventType.DECRYPTION_FAILED), received.map { it.type })
        assertEquals("alice", received.single().userId)
    }

    @Test
    fun `the SDK listener receives native events`() {
        val received = mutableListOf<E2EEEvent>()
        manager().setInternalEventListener { received += it }

        emit()

        assertEquals(listOf(E2EEEventType.DECRYPTION_FAILED), received.map { it.type })
    }

    @Test
    fun `registering the SDK listener does not displace the app listener`() {
        val app = mutableListOf<E2EEEvent>()
        val sdk = mutableListOf<E2EEEvent>()
        val manager = manager()

        manager.setEventListener { app += it }
        manager.setInternalEventListener { sdk += it }
        emit()

        // Both sides asked to observe, and native only has one slot to give.
        assertEquals(1, app.size)
        assertEquals(1, sdk.size)
    }

    @Test
    fun `registering the app listener does not displace the SDK listener`() {
        val app = mutableListOf<E2EEEvent>()
        val sdk = mutableListOf<E2EEEvent>()
        val manager = manager()

        manager.setInternalEventListener { sdk += it }
        manager.setEventListener { app += it }
        emit()

        assertEquals(1, app.size)
        assertEquals(1, sdk.size)
    }

    @Test
    fun `the native observer is claimed once and never re-registered`() {
        val manager = manager()

        manager.setEventListener { }
        manager.setEventListener { }
        manager.setInternalEventListener { }

        // Re-registering would be the bug: whoever called last would own the slot alone.
        verify(exactly = 1) { native.setObserver(any()) }
    }

    @Test
    fun `clearing the app listener leaves the SDK listener observing`() {
        val sdk = mutableListOf<E2EEEvent>()
        val manager = manager()
        manager.setInternalEventListener { sdk += it }
        manager.setEventListener { }

        manager.setEventListener(null)
        emit()

        assertEquals(1, sdk.size)
    }

    @Test
    fun `clearing the SDK listener leaves the app listener observing`() {
        val app = mutableListOf<E2EEEvent>()
        val manager = manager()
        val sdkListener = E2EEEventListener { }
        manager.setInternalEventListener(sdkListener)
        manager.setEventListener { app += it }

        manager.clearInternalEventListener(sdkListener)
        emit()

        assertEquals(1, app.size)
    }

    @Test
    fun `a stale session does not clear the SDK listener a newer one installed`() {
        val current = mutableListOf<E2EEEvent>()
        val manager = manager()
        val staleListener = E2EEEventListener { }
        manager.setInternalEventListener(staleListener)

        // A rejoin or migration builds a new session, which registers over the old one.
        manager.setInternalEventListener { current += it }
        manager.clearInternalEventListener(staleListener)
        emit()

        assertEquals(1, current.size)
    }

    @Test
    fun `a throwing listener does not stop the other from receiving`() {
        val app = mutableListOf<E2EEEvent>()
        val manager = manager()
        manager.setInternalEventListener { error("listener blew up") }
        manager.setEventListener { app += it }

        emit()

        // Both run on the WebRTC thread that produced the event, so one must not starve the other.
        assertEquals(1, app.size)
    }

    @Test
    fun `disposing stops delivering to both listeners`() {
        val app = mutableListOf<E2EEEvent>()
        val sdk = mutableListOf<E2EEEvent>()
        val manager = manager()
        manager.setEventListener { app += it }
        manager.setInternalEventListener { sdk += it }

        manager.dispose()
        disposed = true
        emit()

        assertTrue(app.isEmpty())
        assertTrue(sdk.isEmpty())
    }

    @Test
    fun `every event type is delivered, not only the failures`() {
        val app = mutableListOf<E2EEEvent>()
        manager().setEventListener { app += it }

        // The SDK only traces failures, but that filter belongs to the tracing listener. Diagnostic
        // events the app opted into must still arrive here.
        emit(nativeEvent(type = EncryptionManager.E2eeEventType.PERF_REPORT))
        emit(nativeEvent(type = EncryptionManager.E2eeEventType.KEY_STATE))

        assertEquals(
            listOf(E2EEEventType.PERF_REPORT, E2EEEventType.KEY_STATE),
            app.map { it.type },
        )
    }
}
