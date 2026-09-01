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

/**
 * A state change reported by [StreamEncryptionManager], most usefully a decryption failure for a
 * specific participant. Encryption problems are otherwise silent — media simply arrives
 * undecodable — so this is the main signal for surfacing "you are on a different key" in a UI.
 *
 * @param name The event name as reported by the native layer, for example a key-missing or
 * decryption-failed marker.
 * @param userId The participant the event concerns, when it is scoped to one.
 * @param reason Extra detail from the native layer, when present.
 */
public data class E2EEEvent(
    val name: String,
    val userId: String?,
    val reason: String?,
)

/** Receives [E2EEEvent]s from [StreamEncryptionManager]. Invoked on a WebRTC internal thread. */
public fun interface E2EEEventListener {
    public fun onEvent(event: E2EEEvent)
}
