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

import io.getstream.log.taggedLogger
import io.getstream.video.android.core.utils.safeCall
import org.webrtc.RtpReceiver
import org.webrtc.RtpSender

/**
 * The SDK's default [E2EEManager], backed by WebRTC's framed AES-GCM encryption. Frames are
 * encrypted before they leave the device and decrypted after they arrive, so the SFU forwards
 * media it cannot read.
 *
 * Typical use, matching the JavaScript and iOS SDKs:
 *
 * ```
 * if (StreamEncryptionManager.isSupported()) {
 *     val e2ee = StreamEncryptionManager.create(myUserId)
 *     e2ee.setSharedKey(keyIndex = 0, key = myKeyBytes)
 *     call.setE2EEManager(e2ee)
 *     call.join()
 * }
 * ```
 *
 * You own the instance you create: keep it for as long as you need to rotate keys, and call
 * [dispose] when you are done with it. The SDK does not dispose managers it did not create, since
 * the same manager is usually reused across rejoins and often across calls.
 *
 * Instances are safe to use from any thread. Key changes take effect on the frames encrypted after
 * they are applied.
 */
public class StreamEncryptionManager private constructor(
    private val binding: NativeE2EEBinding,
    private val nativeManager: Any,
    /** The local user these keys and outgoing frames belong to. */
    public val userId: String,
) : E2EEManager, E2EEKeyProvider {

    public companion object {
        /**
         * Whether the WebRTC build bundled with this app supports end-to-end encryption. Check
         * this before [create]; on a runtime without support, encryption cannot be enabled and the
         * call should be joined unencrypted rather than joined with silently-plaintext media.
         */
        @JvmStatic
        public fun isSupported(): Boolean = NativeE2EEBinding.instance != null

        /**
         * Creates a manager for [userId], which must be the local user's ID — it is the identity
         * outgoing frames are encrypted under, and the one remote peers look up keys by.
         *
         * @throws IllegalStateException if [isSupported] is false.
         */
        @JvmStatic
        public fun create(userId: String): StreamEncryptionManager {
            val binding = checkNotNull(NativeE2EEBinding.instance) {
                "End-to-end encryption is not available in this WebRTC build. " +
                    "Guard calls to create() with StreamEncryptionManager.isSupported()."
            }
            val nativeManager = checkNotNull(binding.create(userId)) {
                "org.webrtc.EncryptionManager.create returned null for user $userId"
            }
            return StreamEncryptionManager(binding, nativeManager, userId)
        }
    }

    private val logger by taggedLogger("Call:E2EE")

    @Volatile
    private var disposed = false

    override fun encrypt(sender: RtpSender, codec: String?, trackType: E2EETrackType?) {
        ifActive("encrypt") {
            logger.d { "[encrypt] trackType: $trackType, codec: $codec" }
            binding.encrypt(nativeManager, sender, codec, trackType)
        }
    }

    override fun decrypt(receiver: RtpReceiver, userId: String, trackType: E2EETrackType?) {
        ifActive("decrypt") {
            logger.d { "[decrypt] userId: $userId, trackType: $trackType" }
            binding.decrypt(nativeManager, receiver, userId, trackType)
        }
    }

    override fun setSharedKey(keyIndex: Int, key: ByteArray) {
        ifActive("setSharedKey") { binding.setSharedKey(nativeManager, keyIndex, key) }
    }

    override fun setKey(userId: String, keyIndex: Int, key: ByteArray) {
        ifActive("setKey") { binding.setKey(nativeManager, userId, keyIndex, key) }
    }

    override fun removeSharedKey(keyIndex: Int) {
        ifActive("removeSharedKey") { binding.removeSharedKey(nativeManager, keyIndex) }
    }

    override fun removeKey(userId: String, keyIndex: Int) {
        ifActive("removeKey") { binding.removeKey(nativeManager, userId, keyIndex) }
    }

    override fun removeAllKeys() {
        ifActive("removeAllKeys") { binding.removeAllKeys(nativeManager) }
    }

    /**
     * Observes encryption state changes, most importantly decryption failures. Pass `null` to stop
     * observing. The listener is invoked on a WebRTC internal thread, so hop to your own
     * dispatcher before touching UI state.
     */
    public fun setEventListener(listener: E2EEEventListener?) {
        ifActive("setEventListener") {
            if (!binding.setObserver(nativeManager, listener)) {
                logger.w { "[setEventListener] not supported by this WebRTC build" }
            }
        }
    }

    /**
     * Releases the native manager and wipes its keys. Subsequent calls on this instance are
     * no-ops, so a manager disposed while a call is still running degrades to unencrypted-looking
     * media rather than crashing. Dispose only once you are done with every call that uses it.
     */
    public fun dispose() {
        if (disposed) return
        disposed = true
        safeCall { binding.dispose(nativeManager) }
        logger.d { "[dispose] released native manager for $userId" }
    }

    /**
     * Native calls after [dispose] would reach freed memory, and reflection failures must not
     * propagate into the publish/subscribe paths, so every entry point funnels through here.
     */
    private inline fun ifActive(operation: String, block: () -> Unit) {
        if (disposed) {
            logger.w { "[$operation] ignored, manager is disposed" }
            return
        }
        try {
            block()
        } catch (e: Throwable) {
            logger.e(e) { "[$operation] failed" }
        }
    }
}
