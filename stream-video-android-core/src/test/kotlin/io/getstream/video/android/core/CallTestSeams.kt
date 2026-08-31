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

package io.getstream.video.android.core

import io.getstream.video.android.core.call.RtcSession
import io.getstream.video.android.core.call.components.CallSessionManager
import io.getstream.android.video.generated.models.CallSettingsResponse
import io.getstream.android.video.generated.models.NoiseCancellationSettings
import io.getstream.android.video.generated.models.OwnCapability
import io.getstream.video.android.core.call.components.CallMediaManager
import io.getstream.video.android.core.call.connection.StreamPeerConnectionFactory
import io.getstream.video.android.core.internal.module.CoordinatorConnectionModule
import io.getstream.video.android.core.internal.network.NetworkStateProvider
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Seeds an active [RtcSession] on a real [Call], for tests that start from an already-joined
 * call (reconnect, migrate, escalation).
 *
 * [CallSessionManager] is the single writer for the session and [Call] deliberately exposes no
 * setter — in production a session only ever appears by joining. Reaching the manager
 * reflectively keeps that write path out of the production API instead of adding a facade
 * method that exists purely for tests.
 */
internal fun Call.injectSession(session: RtcSession?) {
    sessionManager().setActiveSession(session)
}

/** Records [sfuId] as a failed edge for this call, as the reconnect flow does on migrate. */
internal fun Call.addFailedSfuId(sfuId: String) = sessionManager().addFailedSfuId(sfuId)

/** Snapshot of the SFU IDs this call has recorded as failed. */
internal fun Call.failedSfuIds(): List<String> = sessionManager().failedSfuIdsSnapshot()

/**
 * Pins the SFU location a call will (re)join, so tests don't depend on a real location lookup.
 */
internal fun Call.injectLocation(location: String?) {
    sessionManager().location = location
}

/**
 * Number of REJOIN / MIGRATE attempts the reconnect loop has made, for tests asserting on
 * escalation behaviour. Read straight off the owning component: only the reconnect flow has any
 * business seeing this counter, so it isn't worth a facade accessor.
 */
internal fun Call.nonFastReconnectAttempts(): Int = sessionManager().nonFastReconnectAttempts

/**
 * Reaches the [CallSessionManager] that owns the session and reconnect bookkeeping.
 *
 * [Call] intentionally exposes none of this: the session has a single write path through
 * [CallSessionManager.setActiveSession], and the rest is bookkeeping shared between the join and
 * reconnect flows. Going through reflection keeps those test-only entry points out of the
 * production API.
 */
private fun Call.sessionManager(): CallSessionManager {
    val field = Call::class.java.getDeclaredField("sessionManager")
    field.isAccessible = true
    return field.get(this) as CallSessionManager
}

/**
 * Replaces the device connectivity provider with a mock reporting [connected].
 *
 * Injects at the connection module rather than at a single component: the reconnect loop reads
 * the provider straight off the module (it deliberately does not go through
 * `CallConnectivityMonitor`, which would close a dependency cycle), so replacing it there is
 * what makes the mock visible to both the loop and the monitor. The monitor's own cached
 * reference is overwritten too, in case it was already resolved.
 */
internal fun Call.injectMockNetwork(connected: Boolean = true) {
    val mockNetwork = mockk<NetworkStateProvider>(relaxed = true)
    every { mockNetwork.isConnected() } returns connected

    val moduleField = CoordinatorConnectionModule::class.java
        .getDeclaredField("networkStateProvider\$delegate")
    moduleField.isAccessible = true
    moduleField.set((client as StreamVideoClient).coordinatorConnectionModule, lazyOf(mockNetwork))

    val monitorField = Call::class.java.getDeclaredField("connectivityMonitor")
    monitorField.isAccessible = true
    val monitor = monitorField.get(this)
    val monitorNetwork = monitor.javaClass.getDeclaredField("network\$delegate")
    monitorNetwork.isAccessible = true
    monitorNetwork.set(monitor, lazyOf(mockNetwork))
}

/**
 * Installs a peer-connection factory on the call's media pipeline, so a test can decide what the
 * shared audio processor reports without any native factory existing.
 *
 * The media component is built inside [Call] and never exposed. Reaching it reflectively keeps a
 * test-only setter out of the production API.
 */
internal fun Call.injectPeerConnectionFactory(factory: StreamPeerConnectionFactory) {
    mediaComponent().peerConnectionFactory = factory
}

private fun Call.mediaComponent(): CallMediaManager {
    val field = Call::class.java.getDeclaredField("media")
    field.isAccessible = true
    return field.get(this) as CallMediaManager
}

/**
 * Applies the wanted audio-processing state to the installed factory, the way building one does in
 * production. Tests install a factory directly, which bypasses that step.
 */
internal fun Call.mediaAppliesWantedState() {
    val media = mediaComponent()
    val field = CallMediaManager::class.java.getDeclaredField("desiredAudioProcessingEnabled")
    field.isAccessible = true
    media.peerConnectionFactory.setAudioProcessingEnabled(field.get(media) as Boolean)
}

/**
 * Seeds the server-driven capability list and call settings a real [CallState] would receive from
 * the coordinator, for tests that exercise policy gates (noise cancellation, permissions).
 *
 * Reflective on purpose: in production both are written only by coordinator responses and events,
 * and that write path should not grow setters that exist solely for tests.
 */
internal fun CallState.injectServerState(
    capabilities: List<OwnCapability>? = null,
    settings: CallSettingsResponse? = null,
) {
    capabilities?.let { mutableStateField<List<OwnCapability>>("_ownCapabilities").value = it }
    settings?.let { mutableStateField<CallSettingsResponse?>("_settings").value = it }
}

/** Builds the minimum settings object the noise-cancellation policy reads. */
internal fun noiseCancellationSettings(
    mode: NoiseCancellationSettings.Mode,
): CallSettingsResponse = mockk(relaxed = true) {
    every { audio.noiseCancellation } returns NoiseCancellationSettings(mode)
}

@Suppress("UNCHECKED_CAST")
private fun <T> CallState.mutableStateField(name: String): MutableStateFlow<T> =
    CallState::class.java.getDeclaredField(name).apply {
        isAccessible = true
    }.get(this) as MutableStateFlow<T>

/** Whether a peer-connection factory has been built for this call yet. */
internal fun Call.hasPeerConnectionFactory(): Boolean {
    val field = CallMediaManager::class.java.getDeclaredField("_peerConnectionFactory")
    field.isAccessible = true
    return field.get(mediaComponent()) != null
}

/** Whether this call still wants audio processing, independently of any factory existing. */
internal fun Call.isAudioProcessingWanted(): Boolean = mediaComponent().isAudioProcessingWanted()
