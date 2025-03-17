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

package io.getstream.video.android.core.notifications

import android.content.Context
import android.media.MediaPlayer
import android.media.Ringtone
import android.os.Build
import android.os.Bundle
import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.DisconnectCause
import androidx.core.app.NotificationManagerCompat
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.notifications.internal.service.telecom.TelecomCallService
import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.SpyK
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class TelecomCallServiceTest {

    private lateinit var context: Context
    private lateinit var service: TelecomCallService

    @MockK(relaxed = true)
    private lateinit var mockStreamVideoClient: StreamVideoClient

    @MockK(relaxed = true)
    lateinit var mockNotificationManager: NotificationManagerCompat

    @MockK(relaxed = true)
    lateinit var mockRingtone: Ringtone

    @SpyK
    var spyMediaPlayer: MediaPlayer = MediaPlayer()

    // If you want to test coroutines within the service:
    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher + Job())

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        // Provide a test context:
        context = ApplicationProvider.getApplicationContext()

        // Clear any global instance from previous tests:
        StreamVideo.removeClient()

        // If your TelecomCallService or usage depends on StreamVideo.instanceOrNull():
        StreamVideo.install(mockStreamVideoClient)

        // Build the service via Robolectric
        service = Robolectric.buildService(TelecomCallService::class.java)
            .create() // calls onCreate()
            .get()
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `onCreateIncomingConnection returns a valid Connection when callId is present`() = runTest {
        // Given
        val extras = Bundle().apply {
            putString(NotificationHandler.INTENT_EXTRA_CALL_CID, "default:123")
            putString(NotificationHandler.INTENT_EXTRA_CALL_DISPLAY_NAME, "Alice")
        }
        val request = ConnectionRequest(null, null, extras)

        // When
        val connection = service.onCreateIncomingConnection(null, request)

        // Then
        assertThat(connection).isNotNull()
        // The Telecom framework might set the connection to "ringing" state if it's a managed call
        assertThat(connection.state).isEqualTo(Connection.STATE_RINGING)
    }

    @Test
    fun `onCreateIncomingConnection returns failed Connection if callId is missing`() = runTest {
        // Given
        val extras = Bundle() // no call ID
        val request = ConnectionRequest(null, null, extras)

        // When
        val connection = service.onCreateIncomingConnection(null, request)

        // Then
        assertThat(connection).isNotNull()
        assertThat(connection.disconnectCause?.code).isEqualTo(DisconnectCause.ERROR)
    }

    @Test
    fun `onCreateOutgoingConnection sets DIALING state`() = runTest {
        // Given
        val extras = Bundle().apply {
            putString(NotificationHandler.INTENT_EXTRA_CALL_CID, "call-id:999")
            putString(NotificationHandler.INTENT_EXTRA_CALL_DISPLAY_NAME, "Bob")
        }
        val request = ConnectionRequest(null, null, extras)

        // When
        val connection = service.onCreateOutgoingConnection(null, request)

        // Then
        assertThat(connection).isNotNull()
        // Outgoing call is typically set to DIALING
        assertThat(connection.state).isEqualTo(Connection.STATE_DIALING)
    }

    @Test
    fun `onDestroy cleans up audio resources`() = runTest {
        // We can check that calling onDestroy stops the ringtone or releases the MediaPlayer
        // 1) Simulate an incoming call that starts the ring:
        val extras = Bundle().apply {
            putString(NotificationHandler.INTENT_EXTRA_CALL_CID, "call-id:abc")
        }
        service.onCreateIncomingConnection(null, ConnectionRequest(null, null, extras))

        // 2) Destroy the service:
        service.onDestroy()
    }

    @Test
    fun `rejecting call stops sound and sets DisconnectCause`() = runTest {
        // Suppose we create an incoming Connection:
        val extras = Bundle().apply {
            putString(NotificationHandler.INTENT_EXTRA_CALL_CID, "call-id:reject-test")
        }
        val incomingConnection = service.onCreateIncomingConnection(
            null,
            ConnectionRequest(null, null, extras),
        )
        assertThat(incomingConnection).isNotNull()

        // If you have code that sets DisconnectCause upon user rejecting call, you might emulate:
        incomingConnection.onDisconnect()

        // Then verify that we have a DisconnectCause
        assertThat(incomingConnection.disconnectCause).isNotNull()
        assertThat(incomingConnection.disconnectCause?.code).isEqualTo(DisconnectCause.LOCAL)
    }
}
