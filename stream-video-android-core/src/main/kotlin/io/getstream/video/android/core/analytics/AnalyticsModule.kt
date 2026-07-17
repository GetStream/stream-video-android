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

package io.getstream.video.android.core.analytics

import io.getstream.android.video.generated.apis.ProductvideoApi
import io.getstream.video.android.core.analytics.coordinator.CoordinatorAnalytics
import io.getstream.video.android.core.analytics.coordinator.CoordinatorAnalyticsStateHolder
import io.getstream.video.android.core.analytics.reporting.ClientEventReporter
import kotlinx.coroutines.CoroutineScope

/**
 * Composition root for video analytics.
 *
 * Owns the single [CoordinatorAnalyticsStateHolder] and injects it into both the
 * writer ([CoordinatorAnalytics]) and the reader ([ClientEventReporter]), so state
 * flows in one direction. Bundling the analytics graph here keeps [StreamVideoClient]'s
 * constructor stable as new analytics objects are added.
 */
internal class AnalyticsModule(
    val clientEventReporter: ClientEventReporter,
    val coordinatorAnalytics: CoordinatorAnalytics,
) {

    companion object {
        fun getDefault(
            api: ProductvideoApi,
            scope: CoroutineScope,
        ): AnalyticsModule {
            val stateHolder = CoordinatorAnalyticsStateHolder()
            val clientEventReporter = ClientEventReporter.getDefault(api, stateHolder)
            return AnalyticsModule(
                clientEventReporter = clientEventReporter,
                coordinatorAnalytics = CoordinatorAnalytics(
                    scope,
                    clientEventReporter,
                    stateHolder,
                ),
            )
        }
    }
}
