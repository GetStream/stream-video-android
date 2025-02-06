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

import android.content.Context
import androidx.core.telecom.CallsManager
import io.getstream.video.android.core.notifications.internal.service.CallServiceConfig
import io.getstream.video.android.core.sounds.CallSoundPlayer
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TelecomHandlerTest {
    private lateinit var telecomHandler: TelecomHandler
    private val context = mockk<Context>()
    private val callManager: CallsManager = mockk(relaxed = true)
    private val callSoundPlayer: CallSoundPlayer = mockk(relaxed = true)
    private val streamCall: StreamCall = mockk(relaxed = true)
    private val callConfig: CallServiceConfig = mockk(relaxed = true)
    private val coroutineScope: CoroutineScope = mockk(relaxed = true)
    private lateinit var telecomCall: TelecomCall

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

        every { streamCall.cid } returns "default:123"

        telecomCall = spyk(
            TelecomCall(streamCall = streamCall, config = callConfig, parentScope = coroutineScope),
        )
    }

    @After
    fun tearDown() {
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

        verify(exactly = 1) { telecomCall.state }
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

        verify(exactly = 1) { telecomHandler["postNotification"](telecomCall) }
        verify(exactly = 1) { telecomHandler["hasNotificationsPermission"]() }
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
}
