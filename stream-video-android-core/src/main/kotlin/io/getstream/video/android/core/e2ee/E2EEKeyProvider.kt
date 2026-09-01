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
 * Key management for an [E2EEManager]. Implementing this alongside [E2EEManager] is optional, and
 * only affects whether the `setKey` / `removeKey` conveniences on
 * [io.getstream.video.android.core.Call] can reach your manager. A manager that sources its keys
 * elsewhere (a hardware keystore, an MLS group, a server handshake) can leave this unimplemented.
 *
 * Keys may be set before joining and rotated at any point during a call — the manager holds them,
 * not the call, so rotation does not require re-joining or re-attaching.
 *
 * Generating and distributing key material is the integrator's responsibility; the SDK only stores
 * what it is given and never transmits it.
 */
public interface E2EEKeyProvider {

    /**
     * Sets the key used for every participant that has no per-user key, including your own
     * outgoing media. Use this when everyone in the call shares one passphrase-derived key.
     *
     * @param keyIndex Slot to write to. Rotating means writing to the next index; peers keep
     * older indices around so in-flight frames encrypted with the previous key still decode.
     * @param key Raw key bytes. AES-128 for the default manager, so 16 bytes.
     */
    public fun setSharedKey(keyIndex: Int, key: ByteArray)

    /**
     * Sets the key for a single participant, which takes precedence over the shared key. Use this
     * when each participant publishes under their own key.
     */
    public fun setKey(userId: String, keyIndex: Int, key: ByteArray)

    /** Drops the shared key at [keyIndex]. */
    public fun removeSharedKey(keyIndex: Int)

    /** Drops [userId]'s key at [keyIndex], falling back to the shared key for that participant. */
    public fun removeKey(userId: String, keyIndex: Int)

    /** Drops every shared and per-user key. Media stops being decodable in both directions. */
    public fun removeAllKeys()
}
