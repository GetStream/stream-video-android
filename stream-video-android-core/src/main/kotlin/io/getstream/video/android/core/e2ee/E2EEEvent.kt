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

import org.webrtc.EncryptionManager

/**
 * A state change reported by [StreamEncryptionManager], most usefully a decryption failure for a
 * specific participant. Encryption problems are otherwise silent — media simply arrives
 * undecodable — so this is the main signal for surfacing "you are on a different key" in a UI.
 *
 * @param type What happened. Prefer branching on this over [name].
 * @param name The event name as reported by the native layer, useful for logs.
 * @param userId The participant the event concerns, when it is scoped to one.
 * @param trackType The track the event concerns, when it is scoped to one.
 * @param keyIndex The key index involved, for key-related events.
 * @param version The frame format version, for [E2EEEventType.UNSUPPORTED_VERSION].
 * @param reason Extra detail from the native layer, when present.
 * @param keyState The current key inventory, populated for [E2EEEventType.KEY_STATE].
 * @param encodePerformance Per-track encrypt timings, populated for [E2EEEventType.PERF_REPORT].
 * @param decodePerformance Per-track decrypt timings, populated for [E2EEEventType.PERF_REPORT].
 */
public data class E2EEEvent(
    val type: E2EEEventType,
    val name: String,
    val userId: String?,
    val trackType: E2EETrackType?,
    val keyIndex: Int?,
    val version: Int?,
    val reason: String?,
    val keyState: E2EEKeyState?,
    val encodePerformance: List<E2EETrackPerformance>,
    val decodePerformance: List<E2EETrackPerformance>,
)

/**
 * The kinds of [E2EEEvent] the native layer reports. [UNKNOWN] covers constants added by a newer
 * WebRTC build than this SDK was compiled against.
 */
public enum class E2EEEventType {
    DECRYPTION_FAILED,
    DECRYPTION_RESUMED,
    DECRYPTION_STALLED,
    ENCRYPTION_FAILED,
    MISSING_KEY,
    UNENCRYPTED_FRAME,
    UNSUPPORTED_VERSION,
    KEY_STATE,
    PERF_REPORT,
    UNKNOWN,
}

/** A snapshot of the keys the manager currently holds. See [StreamEncryptionManager.requestKeyState]. */
public data class E2EEKeyState(
    val perUserKeys: List<E2EEUserKey>,
    val sharedKeys: List<E2EESharedKey>,
)

/**
 * A key held for one participant. [fingerprint] is a digest, not the key material, and is safe to
 * log — it is there so two devices can confirm they agree without exchanging keys.
 */
public data class E2EEUserKey(
    val userId: String,
    val keyIndex: Int,
    val fingerprint: String,
)

/** A key held for every participant. [isActive] marks the index used for outgoing frames. */
public data class E2EESharedKey(
    val keyIndex: Int,
    val fingerprint: String,
    val isActive: Boolean,
)

/**
 * Crypto cost for one track, reported while
 * [StreamEncryptionManager.enablePerformanceReporting] is on.
 *
 * @param maxCryptoMs The worst-case per-frame time in the reporting window. Compare it against the
 * frame budget implied by [fps] to tell whether encryption is what is dropping frames.
 */
public data class E2EETrackPerformance(
    val userId: String,
    val trackType: E2EETrackType?,
    val codec: String?,
    val fps: Double,
    val maxCryptoMs: Double,
)

/** Receives [E2EEEvent]s from [StreamEncryptionManager]. Invoked on a WebRTC internal thread. */
public fun interface E2EEEventListener {
    public fun onEvent(event: E2EEEvent)
}

internal fun EncryptionManager.E2eeEvent.toE2EEEvent(): E2EEEvent = E2EEEvent(
    type = type.toE2EEEventType(),
    name = name,
    userId = userId,
    trackType = trackType?.toE2EETrackType(),
    keyIndex = keyIndex,
    version = version,
    reason = reason,
    keyState = keyState?.toE2EEKeyState(),
    encodePerformance = encode?.map { it.toE2EETrackPerformance() }.orEmpty(),
    decodePerformance = decode?.map { it.toE2EETrackPerformance() }.orEmpty(),
)

internal fun EncryptionManager.E2eeEventType?.toE2EEEventType(): E2EEEventType = when (this) {
    EncryptionManager.E2eeEventType.DECRYPTION_FAILED -> E2EEEventType.DECRYPTION_FAILED
    EncryptionManager.E2eeEventType.DECRYPTION_RESUMED -> E2EEEventType.DECRYPTION_RESUMED
    EncryptionManager.E2eeEventType.DECRYPTION_STALLED -> E2EEEventType.DECRYPTION_STALLED
    EncryptionManager.E2eeEventType.ENCRYPTION_FAILED -> E2EEEventType.ENCRYPTION_FAILED
    EncryptionManager.E2eeEventType.MISSING_KEY -> E2EEEventType.MISSING_KEY
    EncryptionManager.E2eeEventType.UNENCRYPTED_FRAME -> E2EEEventType.UNENCRYPTED_FRAME
    EncryptionManager.E2eeEventType.UNSUPPORTED_VERSION -> E2EEEventType.UNSUPPORTED_VERSION
    EncryptionManager.E2eeEventType.KEY_STATE -> E2EEEventType.KEY_STATE
    EncryptionManager.E2eeEventType.PERF_REPORT -> E2EEEventType.PERF_REPORT
    null -> E2EEEventType.UNKNOWN
}

private fun EncryptionManager.KeyStateReport.toE2EEKeyState(): E2EEKeyState = E2EEKeyState(
    perUserKeys = perUserKeys?.map {
        E2EEUserKey(
            it.userId,
            it.keyIndex,
            it.fingerprint,
        )
    }.orEmpty(),
    sharedKeys = sharedKeys?.map {
        E2EESharedKey(it.keyIndex, it.fingerprint, it.isActive)
    }.orEmpty(),
)

private fun EncryptionManager.TrackPerf.toE2EETrackPerformance(): E2EETrackPerformance =
    E2EETrackPerformance(
        userId = userId,
        trackType = trackType?.toE2EETrackType(),
        codec = codec,
        fps = fps,
        maxCryptoMs = maxCryptoMs,
    )
