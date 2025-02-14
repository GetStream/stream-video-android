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

package io.getstream.video.android.core.telecom

import android.app.NotificationManager
import android.content.Context
import android.telecom.DisconnectCause
import androidx.core.app.NotificationManagerCompat
import androidx.core.telecom.CallControlScope
import androidx.core.telecom.CallsManager
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.audio.StreamAudioDevice
import io.getstream.video.android.core.notifications.DefaultStreamIntentResolver
import io.getstream.video.android.core.notifications.internal.service.CallServiceConfig
import io.getstream.video.android.core.sounds.CallSoundPlayer
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.spyk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TelecomHandlerTest {
    private lateinit var telecomHandler: TelecomHandler
    private val context = mockk<Context>(relaxed = true)
    private val callManager: CallsManager = mockk(relaxed = true)
    private val callSoundPlayer: CallSoundPlayer = mockk(relaxed = true)
    private val streamCall: StreamCall = mockk(relaxed = true)
    private val callConfig: CallServiceConfig = mockk(relaxed = true)
    private val intentResolver: DefaultStreamIntentResolver = mockk(relaxed = true)
    private lateinit var telecomCall: TelecomCall
    private val streamVideo: StreamVideoClient = mockk(relaxed = true)
    private val notificationManager = mockk<NotificationManager>(relaxed = true)

    @Before
    fun setUp() {
        telecomHandler = spyk(
            TelecomHandler(
                applicationContext = context,
                callManager = callManager,
                callSoundPlayer = callSoundPlayer,
            ),
            recordPrivateCalls = true,
        )
        every { telecomHandler["hasNotificationsPermission"]() } returns true

        every { streamCall.cid } returns "default:123"

        telecomCall = spyk(
            TelecomCall(
                streamCall = streamCall,
                config = callConfig,
                intentResolver = intentResolver,
                telecomHandler = telecomHandler,
            ),
        )

        mockkObject(StreamVideo)
        every { StreamVideo.instanceOrNull() } returns streamVideo

        coEvery {
            callManager.addCall(
                telecomCall.attributes,
                any<suspend (Int) -> Unit>(),
                any<suspend (DisconnectCause) -> Unit>(),
                any<suspend () -> Unit>(),
                any<suspend () -> Unit>(),
                any<CallControlScope.() -> Unit>(),
            )
        } just runs

        every { context.getSystemService(Context.NOTIFICATION_SERVICE) } returns notificationManager
        mockkConstructor(NotificationManagerCompat::class)
        every { anyConstructed<NotificationManagerCompat>().notify(any(), any()) } just runs
        every { anyConstructed<NotificationManagerCompat>().cancel(any()) } just runs
    }

    @Test
    fun registerCall_newCall_addsCallToInternalMap() {
        val callsMap = getPrivateCallsMap()

        telecomHandler.registerCall(streamCall, callConfig, false)
        val entry = callsMap[streamCall.cid]

        assertNotNull(entry)
        assert(entry.streamCall == streamCall)
    }

    private fun getPrivateCallsMap(): MutableMap<String, TelecomCall> {
        val callsField = TelecomHandler::class.java.getDeclaredField("calls")
        callsField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val callsMap = callsField.get(telecomHandler) as MutableMap<String, TelecomCall>

        return callsMap
    }

    @Test
    fun registerCall_existingCall_doesNotAddCallToInternalMap() {
        val callsMap = getPrivateCallsMap()

        telecomHandler.registerCall(streamCall, callConfig, false)
        telecomHandler.registerCall(streamCall, callConfig, false)
        val entries = callsMap.filter { it.key == streamCall.cid }

        assert(callsMap.size == 1)
        assert(entries.size == 1)
    }

    @Test
    fun registerCall_incomingCall_preparesIncomingCall() {
        val isIncomingCall = true
        telecomHandler.registerCall(streamCall, callConfig, isIncomingCall)

        verify(exactly = 1) { telecomHandler["prepareIncomingCall"](streamCall) }
    }

    @Test
    fun changeCallState_newState_setsCallState() {
        val calls = getPrivateCallsMap()
        calls[streamCall.cid] = telecomCall
        val newState = TelecomCallState.ONGOING

        telecomHandler.changeCallState(streamCall, newState)

        verify(exactly = 1) { telecomCall.state = newState }
        assertEquals(newState, telecomCall.state)
        assertEquals(newState, calls[streamCall.cid]?.state)
    }

    @Test
    fun changeCallState_newStateAndInexistentCall_ignoresState() {
        val newState = TelecomCallState.IDLE

        telecomHandler.changeCallState(streamCall, newState)

        verify(exactly = 0) { telecomCall.state }
    }

    @Test
    fun changeCallState_newState_postsNotification() {
        val calls = getPrivateCallsMap()
        calls[streamCall.cid] = telecomCall
        val newState = TelecomCallState.ONGOING

        telecomHandler.changeCallState(streamCall, newState)

        verify(exactly = 1) {
            telecomHandler["postNotification"](telecomCall)
            telecomHandler["hasNotificationsPermission"]()
        }
    }

    @Test
    fun changeCallState_sameState_doesNotPostNotification() {
        val calls = getPrivateCallsMap()
        calls[streamCall.cid] = telecomCall.apply { state = TelecomCallState.ONGOING }
        val newState = TelecomCallState.ONGOING

        telecomHandler.changeCallState(streamCall, newState)

        verify(exactly = 0) { telecomHandler["postNotification"](any<TelecomCall>()) }
    }

    @Test
    fun changeCallState_newStateAndInexistentCall_doesNotPostNotification() {
        val newState = TelecomCallState.IDLE

        telecomHandler.changeCallState(streamCall, newState)

        verify(exactly = 0) { telecomHandler["postNotification"](any<TelecomCall>()) }
    }

    @Test
    fun changeCallState_fromIdleToIncoming_addsCallToTelecom() {
        val calls = getPrivateCallsMap()
        calls[streamCall.cid] = telecomCall.apply { state = TelecomCallState.IDLE }
        val newState = TelecomCallState.INCOMING

        telecomHandler.changeCallState(streamCall, newState)

        coVerify(exactly = 1) {
            callManager.addCall(
                telecomCall.attributes,
                any<suspend (Int) -> Unit>(),
                any<suspend (DisconnectCause) -> Unit>(),
                any<suspend () -> Unit>(),
                any<suspend () -> Unit>(),
                any<CallControlScope.() -> Unit>(),
            )
        }
    }

    @Test
    fun changeCallState_fromIdleToOutgoing_addsCallToTelecom() {
        val calls = getPrivateCallsMap()
        calls[streamCall.cid] = telecomCall.apply { state = TelecomCallState.IDLE }
        val newState = TelecomCallState.OUTGOING

        telecomHandler.changeCallState(streamCall, newState)

        coVerify(exactly = 1) {
            callManager.addCall(
                telecomCall.attributes,
                any<suspend (Int) -> Unit>(),
                any<suspend (DisconnectCause) -> Unit>(),
                any<suspend () -> Unit>(),
                any<suspend () -> Unit>(),
                any<CallControlScope.() -> Unit>(),
            )
        }
    }

    @Test
    fun changeCallState_fromIncomingToOngoing_doesNotAddCallToTelecom() {
        val calls = getPrivateCallsMap()
        calls[streamCall.cid] = telecomCall.apply { state = TelecomCallState.INCOMING }
        val newState = TelecomCallState.ONGOING

        telecomHandler.changeCallState(streamCall, newState)

        coVerify(exactly = 0) {
            callManager.addCall(
                telecomCall.attributes,
                any<suspend (Int) -> Unit>(),
                any<suspend (DisconnectCause) -> Unit>(),
                any<suspend () -> Unit>(),
                any<suspend () -> Unit>(),
                any<CallControlScope.() -> Unit>(),
            )
        }
    }

    @Test
    fun changeCallState_fromOutgoingToOngoing_doesNotAddCallToTelecom() {
        val calls = getPrivateCallsMap()
        calls[streamCall.cid] = telecomCall.apply { state = TelecomCallState.OUTGOING }
        val newState = TelecomCallState.ONGOING

        telecomHandler.changeCallState(streamCall, newState)

        coVerify(exactly = 0) {
            callManager.addCall(
                telecomCall.attributes,
                any<suspend (Int) -> Unit>(),
                any<suspend (DisconnectCause) -> Unit>(),
                any<suspend () -> Unit>(),
                any<suspend () -> Unit>(),
                any<CallControlScope.() -> Unit>(),
            )
        }
    }

//    @Test
//    fun changeCallState_fromIdleToIncoming_playsCallSound() {
//        val calls = getPrivateCallsMap()
//        calls[streamCall.cid] = telecomCall.apply { state = TelecomCallState.IDLE }
//        val newState = TelecomCallState.INCOMING
//        every { telecomHandler["hasNotificationsPermission"]() } returns true
//
//        telecomHandler.changeCallState(streamCall, newState)
//
//        assert(telecomCall.state == newState)
//        verify(exactly = 1) { telecomHandler["postNotification"](any<TelecomCall>()) }
//        verify(exactly = 1) { telecomHandler["hasNotificationsPermission"]() }
//        verify(exactly = 1) { streamVideo.getRingingCallNotification(
//            any<RingingState>(),
//            any<StreamCallId>(),
//            any<String>(),
//            any<Boolean>(),
//        ) }
//        verify(exactly = 1) { callSoundPlayer.playCallSound(any<Uri>()) }
//    }

    @Test
    fun setDeviceListener_existingCall_setsDeviceListenerAndSendsInitialDevices() {
        val calls = getPrivateCallsMap()
        calls[streamCall.cid] = telecomCall
        val deviceListener: DeviceListener = mockk(relaxed = true)
        every { telecomCall.deviceListener = any() } just runs // Stub setter

        telecomHandler.setDeviceListener(streamCall, deviceListener)

        verify(exactly = 1) {
            telecomCall.deviceListener = deviceListener
            telecomHandler["sendInitialDevices"](deviceListener)
            callManager.getAvailableStartingCallEndpoints()
            deviceListener.invoke(any<List<StreamAudioDevice>>(), any<StreamAudioDevice>())
        }
    }

    @Test
    fun setDeviceListener_inexistentCall_doesNotSetDeviceListenerButSendsInitialDevices() {
        val deviceListener: DeviceListener = mockk(relaxed = true)

        telecomHandler.setDeviceListener(streamCall, deviceListener)

        verify(exactly = 0) {
            telecomCall.deviceListener = any<DeviceListener>()
        }
        verify(exactly = 1) {
            telecomHandler["sendInitialDevices"](any<DeviceListener>())
            callManager.getAvailableStartingCallEndpoints()
            deviceListener.invoke(any<List<StreamAudioDevice>>(), any<StreamAudioDevice>())
        }
    }
}
