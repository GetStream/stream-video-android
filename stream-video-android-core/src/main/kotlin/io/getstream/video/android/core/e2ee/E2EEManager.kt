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

import org.webrtc.RtpReceiver
import org.webrtc.RtpSender

/**
 * Attaches end-to-end encryption to the media that flows through a call.
 *
 * The SDK ships [StreamEncryptionManager] as the default AES-GCM implementation. Integrators who
 * need a different scheme (a different cipher, a regulator-mandated key hierarchy, a hardware
 * keystore) implement this interface themselves and hand it to
 * [io.getstream.video.android.core.Call.setE2EEManager] before joining. The SDK then never sees
 * the key material or the crypto, it only tells the manager which senders and receivers exist.
 *
 * Key generation and key distribution are deliberately **not** part of this interface. Stream's
 * infrastructure must stay isolated from that process, so it is the integrator's responsibility.
 * Keys therefore live on the manager instance, not on the call: see
 * [StreamEncryptionManager.setSharedKey] and [StreamEncryptionManager.setKey] for the default
 * manager's surface, and give your own manager whichever key API suits your scheme.
 *
 * This mirrors the `E2EEManager` interface in the JavaScript SDK and `StreamE2EEManager` in the
 * iOS SDK, so the same integration shape works across all three platforms.
 */
public interface E2EEManager {

    /**
     * Called once per outgoing track, right after the publisher adds its transceiver. The
     * implementation is expected to install a frame encryptor on [sender].
     *
     * @param sender The sender carrying the local track.
     * @param codec The negotiated codec, lowercased and without a MIME prefix (`opus`, `vp8`,
     * `av1`), or `null` when the SDK cannot determine it. Not restricted to a fixed set;
     * implementations that do not need a codec hint may ignore it.
     * @param trackType Which kind of track this is, or `null` when it cannot be mapped.
     * @return Success when the frame encryptor was attached, or a failure that prevents the SDK
     * from publishing this sender without encryption.
     */
    public fun encrypt(
        sender: RtpSender,
        codec: String?,
        trackType: E2EETrackType?,
    ): Result<Unit>

    /**
     * Called once per incoming track, when the subscriber learns which participant it belongs to.
     * The implementation is expected to install a frame decryptor on [receiver].
     *
     * @param receiver The receiver carrying the remote track.
     * @param userId The user the track belongs to. Keys are looked up per user, so a wrong value
     * here surfaces as undecryptable media rather than as an error.
     * @param trackType Which kind of track this is, or `null` when it cannot be mapped.
     * @return Success when the frame decryptor was attached, or a failure that leaves the track
     * available for a later attachment attempt.
     */
    public fun decrypt(
        receiver: RtpReceiver,
        userId: String,
        trackType: E2EETrackType?,
    ): Result<Unit>
}
