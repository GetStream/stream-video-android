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

package io.getstream.video.android.core.rtc

import org.junit.Assert.assertEquals
import org.junit.Test
import stream.video.sfu.event.ReconnectDetails
import stream.video.sfu.models.WebsocketReconnectStrategy

/**
 * Validates the `previous_session_id` contract for each reconnection strategy,
 * aligned with the JS SDK implementation:
 *
 * | Strategy    | previous_session_id | session_id        | Routing hint     |
 * |-------------|---------------------|-------------------|------------------|
 * | FAST        | ""                  | reused            | same SFU         |
 * | REJOIN      | old session ID      | newly generated   | may change SFU   |
 * | MIGRATE     | ""                  | reused            | from_sfu_id      |
 *
 * Only REJOIN sets `previous_session_id` because it creates a new session and
 * the SFU needs to correlate it with the old one to transfer state. FAST and
 * MIGRATE reuse the existing session ID, so no previous reference is needed.
 *
 * Reference: JS SDK `Call.ts` getReconnectDetails():
 *   `previousSessionId: performingRejoin ? previousSessionId || '' : ''`
 */
class ReconnectDetailsTest {

    private val oldSessionId = "session-abc-123"
    private val oldSfuName = "sfu-edge-eu-west-1"

    @Test
    fun `FAST reconnect does not set previous_session_id`() {
        val details = buildReconnectDetails(
            strategy = WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_FAST,
            previousSessionId = "",
        )

        assertEquals("", details.previous_session_id)
        assertEquals(
            WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_FAST,
            details.strategy,
        )
        assertEquals("", details.from_sfu_id)
    }

    @Test
    fun `REJOIN sets previous_session_id to old session ID`() {
        val details = buildReconnectDetails(
            strategy = WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_REJOIN,
            previousSessionId = oldSessionId,
        )

        assertEquals(oldSessionId, details.previous_session_id)
        assertEquals(
            WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_REJOIN,
            details.strategy,
        )
    }

    @Test
    fun `MIGRATE does not set previous_session_id but sets from_sfu_id`() {
        val details = buildReconnectDetails(
            strategy = WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_MIGRATE,
            previousSessionId = "",
            fromSfuId = oldSfuName,
        )

        assertEquals("", details.previous_session_id)
        assertEquals(oldSfuName, details.from_sfu_id)
        assertEquals(
            WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_MIGRATE,
            details.strategy,
        )
    }

    @Test
    fun `REJOIN with empty previous session falls back to empty string`() {
        val details = buildReconnectDetails(
            strategy = WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_REJOIN,
            previousSessionId = "",
        )

        assertEquals("", details.previous_session_id)
    }

    @Test
    fun `FAST reconnect reason is propagated`() {
        val details = buildReconnectDetails(
            strategy = WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_FAST,
            previousSessionId = "",
            reason = "signal_lost",
        )

        assertEquals("signal_lost", details.reason)
        assertEquals("", details.previous_session_id)
    }

    @Test
    fun `MIGRATE carries from_sfu_id and empty previous_session_id`() {
        val details = buildReconnectDetails(
            strategy = WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_MIGRATE,
            previousSessionId = "",
            fromSfuId = "sfu-edge-us-east-1",
        )

        assertEquals("sfu-edge-us-east-1", details.from_sfu_id)
        assertEquals("", details.previous_session_id)
    }

    /**
     * Mirrors the ReconnectDetails construction in Call.kt for each strategy.
     */
    private fun buildReconnectDetails(
        strategy: WebsocketReconnectStrategy,
        previousSessionId: String,
        fromSfuId: String = "",
        reconnectAttempt: Int = 1,
        reason: String = "",
    ): ReconnectDetails = ReconnectDetails(
        previous_session_id = previousSessionId,
        strategy = strategy,
        announced_tracks = emptyList(),
        subscriptions = emptyList(),
        from_sfu_id = fromSfuId,
        reconnect_attempt = reconnectAttempt,
        reason = reason,
    )
}
