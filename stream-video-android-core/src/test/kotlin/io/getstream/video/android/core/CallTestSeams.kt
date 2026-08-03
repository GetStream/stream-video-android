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
import io.getstream.video.android.core.internal.module.CoordinatorConnectionModule
import io.getstream.video.android.core.internal.network.NetworkStateProvider
import io.mockk.every
import io.mockk.mockk

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
    val field = Call::class.java.getDeclaredField("sessionManager")
    field.isAccessible = true
    (field.get(this) as CallSessionManager).setActiveSession(session)
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
