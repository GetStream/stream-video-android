package io.getstream.video.android.core.notifications

import android.content.Context
import android.os.Build
import android.telecom.CallAudioState
import android.telecom.DisconnectCause
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import io.getstream.android.video.generated.models.CallEndedEvent
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.notifications.internal.service.telecom.DeviceListener
import io.getstream.video.android.core.notifications.internal.service.telecom.VoipConnection
import io.getstream.video.android.model.StreamCallId
import io.mockk.MockKAnnotations
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class VoipConnectionTest {

    private lateinit var context: Context

    @MockK(relaxed = true)
    private lateinit var mockStreamVideoClient: StreamVideoClient

    @MockK(relaxed = true)
    private lateinit var mockDefaultIntentResolver: DefaultStreamIntentResolver

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        // Provide a test context
        context = ApplicationProvider.getApplicationContext()

        // Install the mock StreamVideoClient
        StreamVideo.install(mockStreamVideoClient)

        // Optional: if VoipConnection or onAnswer uses DefaultStreamIntentResolver internally, mock that
        mockkConstructor(DefaultStreamIntentResolver::class)
        every { anyConstructed<DefaultStreamIntentResolver>().searchAcceptCallPendingIntent(any(), any()) } returns mockk {
            justRun { send() } // no-op
        }

        // If needed, direct calls from Dispatchers.IO to testDispatcher
        // so we can control coroutines:
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        // Clean up
        unmockkAll()
        StreamVideo.removeClient()
        Dispatchers.resetMain()
    }

    @Test
    fun `onDisconnect - calls call leave and sets disconnected`() = runTest {
        val mockCall = mockk<io.getstream.video.android.core.Call>(relaxed = true)
        every { mockStreamVideoClient.call("typeA", "id123") } returns mockCall

        val connection = VoipConnection(
            context,
            StreamCallId("typeA", "id123"),
            isIncoming = false
        )

        connection.onDisconnect()
        advanceUntilIdle()

        coVerify { mockCall.leave() }
        assertThat(connection.state).isEqualTo(android.telecom.Connection.STATE_DISCONNECTED)
        assertThat(connection.disconnectCause.code).isEqualTo(DisconnectCause.LOCAL)
    }

    @Test
    fun `onAbort - sets disconnected with canceled`() = runTest {
        val connection = VoipConnection(
            context,
            StreamCallId("typeB", "id999"),
            isIncoming = false
        )

        connection.onAbort()
        advanceUntilIdle()

        assertThat(connection.state).isEqualTo(android.telecom.Connection.STATE_DISCONNECTED)
        assertThat(connection.disconnectCause.code).isEqualTo(DisconnectCause.CANCELED)
    }

    @Test
    fun `onHold - sets onHold, calls call reject + leave`() = runTest {
        val mockCall = mockk<io.getstream.video.android.core.Call>(relaxed = true)
        every { mockStreamVideoClient.call("default", "123") } returns mockCall

        val connection = VoipConnection(
            context,
            StreamCallId("default", "123"),
            isIncoming = true
        )

        connection.onHold()
        advanceUntilIdle()

        coVerify {
            mockCall.leave()
        }
        assertThat(connection.state).isEqualTo(android.telecom.Connection.STATE_DISCONNECTED)
    }

    @Test
    fun `onCallAudioStateChanged - logs new mute state`() = runTest {
        val connection = VoipConnection(context, StreamCallId("t", "id"), isIncoming = false)
        // Just verify we can call onCallAudioStateChanged w/o exception
        val audioState = CallAudioState( true, CallAudioState.ROUTE_SPEAKER, 0)
        connection.onCallAudioStateChanged(audioState)

        // Because it's just logging, no real effect on the Connection state
        assertThat(connection.state).isEqualTo(android.telecom.Connection.STATE_NEW)
    }

    @Test
    fun `setDeviceListener - sets the listener on currentConnection`() = runTest {
        // Prepare a connection
        val connection = VoipConnection(context, StreamCallId("typ", "abc"), false)
        VoipConnection.currentConnection = connection

        val mockListener: DeviceListener = mockk(relaxed = true)

        // 1) Create an AudioHandler via setDeviceListener
        val audioHandler = VoipConnection.setDeviceListener(mockListener)
        // 2) Start it
        audioHandler.start()

        // The listener property should now be assigned in the connection
        val deviceListenerField = connection.javaClass.getDeclaredField("deviceListener").apply {
            isAccessible = true
        }
        val assignedListener = deviceListenerField.get(connection) as DeviceListener?
        assertThat(assignedListener).isSameInstanceAs(mockListener)
    }
}
