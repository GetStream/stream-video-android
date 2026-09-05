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

import io.getstream.log.StreamLog
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.utils.safeCall
import org.webrtc.EncryptionManager
import org.webrtc.RtpReceiver
import org.webrtc.RtpSender
import java.util.concurrent.atomic.AtomicReference

/**
 * The SDK's default [E2EEManager], backed by WebRTC's framed AES-GCM encryption. Frames are
 * encrypted before they leave the device and decrypted after they arrive, so the SFU forwards
 * media it cannot read.
 *
 * Typical use, matching the JavaScript and iOS SDKs:
 *
 * ```
 * StreamEncryptionManager.create(myUserId).onSuccess { e2ee ->
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
    private val native: EncryptionManager,
) : E2EEManager {

    public companion object {
        /**
         * Whether the WebRTC build resolved at runtime supports end-to-end encryption. Check this
         * before [create]; on a runtime without support, encryption cannot be enabled and the call
         * should be joined unencrypted rather than joined with silently-plaintext media.
         *
         * This can only be false if the app overrides the WebRTC dependency with a build older
         * than the one this SDK compiles against, in which case the class does not link.
         */
        @JvmStatic
        public fun isSupported(): Boolean = try {
            EncryptionManager.isSupported()
        } catch (e: LinkageError) {
            StreamLog.w("Call:E2EE") { "org.webrtc.EncryptionManager is unavailable: ${e.message}" }
            false
        }

        /**
         * Creates a manager for [userId], which must be the local user's ID — it is the identity
         * outgoing frames are encrypted under, and the one remote peers look up keys by.
         *
         * Call this after the [io.getstream.video.android.core.StreamVideo] client is built: it
         * allocates native state, so it needs WebRTC's native library loaded.
         *
         * @param algorithm The AES-GCM variant to use. Every participant must agree on it, and on
         * a matching key length, or frames will not decode.
         * @return The manager, or a failure when this WebRTC build does not support encryption or
         * native manager allocation fails.
         */
        @JvmStatic
        public fun create(
            userId: String,
            algorithm: E2EEAlgorithm = E2EEAlgorithm.AES_128_GCM,
        ): Result<StreamEncryptionManager> {
            if (!isSupported()) {
                return Result.failure(
                    IllegalStateException(
                        "End-to-end encryption is not available in this WebRTC build.",
                    ),
                )
            }
            return runCatching {
                StreamEncryptionManager(
                    EncryptionManager.create(userId, algorithm.toNativeAlgorithm()),
                )
            }
        }
    }

    private val logger by taggedLogger("Call:E2EE")

    /** The local user these keys and outgoing frames belong to. */
    public val userId: String get() = native.userId()

    /** The AES-GCM variant this manager encrypts with. */
    public val algorithm: E2EEAlgorithm get() = native.algorithm().toE2EEAlgorithm()

    private val appListener = AtomicReference<E2EEEventListener?>(null)
    private val internalListener = AtomicReference<E2EEEventListener?>(null)

    init {
        // Native offers a single observer slot, but both the app and the SDK need these events:
        // the app to surface key problems in its UI, the SDK to trace them into call stats. Claim
        // the slot once here and fan out, so neither side can unregister the other.
        ifActive("installObserver") {
            native.setObserver(
                EncryptionManager.Observer { event ->
                    val mapped = event.toE2EEEvent()
                    dispatch(internalListener.get(), mapped, "sdk")
                    dispatch(appListener.get(), mapped, "app")
                },
            )
        }
    }

    /**
     * Delivers [event] without letting one listener's failure starve the other, since both run on
     * the WebRTC thread that produced the event.
     */
    private fun dispatch(target: E2EEEventListener?, event: E2EEEvent, label: String) {
        if (target == null) return
        try {
            target.onEvent(event)
        } catch (e: Throwable) {
            logger.e(e) { "[dispatch] $label listener threw for ${event.type}" }
        }
    }

    /**
     * Lets the SDK observe encryption events alongside the app's own listener, for tracing into
     * call stats. Pass `null` to stop. Not part of the public surface: apps use [setEventListener].
     */
    internal fun setInternalEventListener(listener: E2EEEventListener?) {
        internalListener.set(listener)
    }

    /**
     * Clears [listener] only if it is still the registered one. Managers outlive sessions, so a
     * finished session must not unregister the listener a newer session already installed.
     */
    internal fun clearInternalEventListener(listener: E2EEEventListener) {
        internalListener.compareAndSet(listener, null)
    }

    override fun encrypt(
        sender: RtpSender,
        codec: String?,
        trackType: E2EETrackType?,
    ): Result<Unit> {
        if (native.isDisposed) {
            return Result.failure(
                IllegalStateException("Cannot attach an encryptor: manager is disposed."),
            )
        }
        return runCatching {
            logger.d { "[encrypt] trackType: $trackType, codec: $codec" }
            native.encrypt(sender, codec, trackType?.toNativeTrackType())
        }.onFailure { error ->
            logger.e(error) { "[encrypt] failed" }
        }
    }

    override fun decrypt(
        receiver: RtpReceiver,
        userId: String,
        trackType: E2EETrackType?,
    ): Result<Unit> {
        if (native.isDisposed) {
            return Result.failure(
                IllegalStateException("Cannot attach a decryptor: manager is disposed."),
            )
        }
        return runCatching {
            logger.d { "[decrypt] userId: $userId, trackType: $trackType" }
            native.decrypt(receiver, userId, trackType?.toNativeTrackType())
        }.onFailure { error ->
            logger.e(error) { "[decrypt] failed" }
        }
    }

    /**
     * Sets the key used for every participant that has no per-user key, including your own
     * outgoing media. Use this when everyone in the call shares one passphrase-derived key.
     *
     * Safe to call during a call to rotate keys: write the new key to the next index, and peers
     * that still hold the old index can decode frames that were already in flight.
     *
     * @param keyIndex Slot to write to.
     * @param key Raw key bytes, sized for [algorithm] — 16 bytes for AES-128-GCM, 32 for
     * AES-256-GCM.
     */
    public fun setSharedKey(keyIndex: Int, key: ByteArray) {
        ifActive("setSharedKey") { native.setSharedKey(keyIndex, key) }
    }

    /**
     * Sets the key for a single participant, which takes precedence over the shared key. Use this
     * when each participant publishes under their own key. See [setSharedKey] for rotation.
     */
    public fun setKey(userId: String, keyIndex: Int, key: ByteArray) {
        ifActive("setKey") { native.setKey(userId, keyIndex, key) }
    }

    /** Drops the shared key at [keyIndex]. */
    public fun removeSharedKey(keyIndex: Int) {
        ifActive("removeSharedKey") { native.removeSharedKey(keyIndex) }
    }

    /** Drops [userId]'s key at [keyIndex], falling back to the shared key for that participant. */
    public fun removeKey(userId: String, keyIndex: Int) {
        ifActive("removeKey") { native.removeKey(userId, keyIndex) }
    }

    /**
     * Drops every key held for [userId], at every index, falling back to the shared key for that
     * participant. Shared keys are untouched — use [removeSharedKey] for those.
     */
    public fun removeAllKeys(userId: String) {
        ifActive("removeAllKeys") { native.removeAllKeys(userId) }
    }

    /**
     * Observes encryption state changes, most importantly decryption failures. Pass `null` to stop
     * observing. The listener is invoked on a WebRTC internal thread, so hop to your own
     * dispatcher before touching UI state.
     */
    public fun setEventListener(listener: E2EEEventListener?) {
        if (native.isDisposed) {
            logger.w { "[setEventListener] ignored, manager is disposed" }
            return
        }
        appListener.set(listener)
    }

    /**
     * Turns on periodic [E2EEEventType.PERF_REPORT] events carrying per-track crypto timings. Off
     * by default; it costs a timing measurement per frame, so leave it off outside diagnostics.
     */
    public fun enablePerformanceReporting(enabled: Boolean) {
        ifActive("enablePerformanceReporting") { native.enablePerformanceReporting(enabled) }
    }

    /**
     * Asks for a [E2EEEventType.KEY_STATE] event describing the keys currently held. Answered
     * asynchronously through the listener registered with [setEventListener], so register first.
     */
    public fun requestKeyState() {
        ifActive("requestKeyState") { native.requestKeyState() }
    }

    /**
     * Releases the native manager and wipes its keys. Subsequent key operations are ignored;
     * attempting to attach an encryptor or decryptor returns a failure so the SDK cannot treat an
     * unprotected track as configured. Dispose only once you are done with every call that uses it.
     */
    public fun dispose() {
        if (native.isDisposed) return
        val disposedUserId = userId
        // Drop both listeners first: nothing should be delivered from a manager being torn down,
        // and holding them would outlive the native state they describe.
        appListener.set(null)
        internalListener.set(null)
        safeCall { native.dispose() }
        logger.d { "[dispose] released native manager for $disposedUserId" }
    }

    /**
     * Native calls after [dispose] would reach freed memory. Key and observer operations are
     * best-effort, so they are ignored or logged here; track attachment uses explicit results.
     */
    private inline fun ifActive(operation: String, block: () -> Unit) {
        if (native.isDisposed) {
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

/** The AES-GCM variants the default manager supports. Determines the required key length. */
public enum class E2EEAlgorithm {
    /** 16-byte keys. */
    AES_128_GCM,

    /** 32-byte keys. */
    AES_256_GCM,
}

internal fun E2EEAlgorithm.toNativeAlgorithm(): EncryptionManager.Algorithm = when (this) {
    E2EEAlgorithm.AES_128_GCM -> EncryptionManager.Algorithm.AES_128_GCM
    E2EEAlgorithm.AES_256_GCM -> EncryptionManager.Algorithm.AES_256_GCM
}

internal fun EncryptionManager.Algorithm.toE2EEAlgorithm(): E2EEAlgorithm = when (this) {
    EncryptionManager.Algorithm.AES_128_GCM -> E2EEAlgorithm.AES_128_GCM
    EncryptionManager.Algorithm.AES_256_GCM -> E2EEAlgorithm.AES_256_GCM
}
