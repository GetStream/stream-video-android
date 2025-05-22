/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core.subscriptions.api

import io.getstream.video.android.core.api.SignalServerService
import io.getstream.video.android.core.subscriptions.impl.DefaultDebouncedSubscriptionManager
import io.getstream.video.android.core.subscriptions.impl.DefaultManualOverridesSubscriptionManager
import io.getstream.video.android.core.subscriptions.impl.DefaultSubscriptionManager
import io.getstream.video.android.core.subscriptions.impl.DefaultViewPortBasedSubscriptionManager
import kotlinx.coroutines.CoroutineScope

/**
 * Creates a default [SubscriptionManager] implementation.
 *
 * @param sessionId The session ID for the manager.
 * @param signalingService The signaling service to use for subscriptions.
 * @return A [SubscriptionManager] instance.
 */
internal fun subscriptionManager(
    sessionId: String,
    signalingService: SignalServerService,
): SubscriptionManager =
    DefaultSubscriptionManager(sessionId, signalingService)

/**
 * Creates a [DebouncedSubscriptionManager] that wraps a [SubscriptionManager] and debounces unsubscribe calls.
 *
 * @param scope The coroutine scope for debounce operations.
 * @param delegate The underlying [SubscriptionManager] to delegate to.
 * @return A [DebouncedSubscriptionManager] instance.
 */
internal fun debouncedSubscriptionManager(
    scope: CoroutineScope,
    delegate: SubscriptionManager,
): DebouncedSubscriptionManager =
    DefaultDebouncedSubscriptionManager(scope, delegate)

/**
 * Creates a [ViewportBasedSubscriptionManager] that manages subscriptions based on viewport logic.
 *
 * @param debouncedManager The debounced manager to delegate to.
 * @return A [ViewportBasedSubscriptionManager] instance.
 */
internal fun viewportBasedSubscriptionManager(
    debouncedManager: DebouncedSubscriptionManager,
): ViewportBasedSubscriptionManager =
    DefaultViewPortBasedSubscriptionManager(debouncedManager)

/**
 * Creates a [ManualOverridesSubscriptionManager] that allows manual overrides for visibility and video dimensions.
 *
 * @param delegate The underlying [ViewportBasedSubscriptionManager] to delegate to.
 * @return A [ManualOverridesSubscriptionManager] instance.
 */
internal fun manualOverridesSubscriptionManager(
    delegate: ViewportBasedSubscriptionManager,
): ManualOverridesSubscriptionManager =
    DefaultManualOverridesSubscriptionManager(delegate)

/**
 * Creates a default [ManualOverridesSubscriptionManager] with the recommended default chain of managers.
 *
 * @param scope The coroutine scope for async operations.
 * @param sessionId The session ID for the manager.
 * @param signalingService The signaling service to use for subscriptions.
 * @return A [ManualOverridesSubscriptionManager] instance with the default manager chain.
 */
internal fun defaultStreamSubscriptionManager(
    scope: CoroutineScope,
    sessionId: String,
    signalingService: SignalServerService,
): ManualOverridesSubscriptionManager =
    manualOverridesSubscriptionManager(
        viewportBasedSubscriptionManager(
            debouncedSubscriptionManager(
                scope,
                subscriptionManager(sessionId, signalingService),
            ),
        ),
    )
