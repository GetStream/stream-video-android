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

package io.getstream.video.android.core.call.components

/**
 * Registers and deregisters a call in the client-level registries — the client's ringing and
 * active call slots, the system telecom bookkeeping, and the client's per-call cleanup.
 *
 * Every operation here needs the `Call` instance itself, which is the one thing the extracted
 * collaborators deliberately don't hold. [io.getstream.video.android.core.Call] implements this
 * as an anonymous object capturing `this`, so the join, API and lifecycle components can drive
 * these transitions without a reference to the call.
 */
internal interface ClientCallRegistry {

    /** Points the client's ringing-call slot at this call. */
    fun markRinging()

    /** Registers this call as an outgoing ringing call. */
    fun registerOutgoingRing()

    /** Promotes this call to the client's active call. */
    fun markActive()

    /** Transitions client state for an incoming call accepted on this device. */
    fun markAccepted()

    /**
     * Removes this call from the ringing / active registries and telecom bookkeeping, and runs
     * the client's per-call cleanup. Called once, while leaving.
     */
    fun detach()
}
