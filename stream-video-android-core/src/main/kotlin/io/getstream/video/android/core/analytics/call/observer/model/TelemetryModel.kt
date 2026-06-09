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

package io.getstream.video.android.core.analytics.call.observer.model

internal data class TelemetryModel(val retryAttempt: Int, val joinReason: JoinReason? = null)
internal sealed class JoinReason {

    abstract val message: String

    data object FirstAttempt : JoinReason() {
        override val message: String = "first-attempt"
    }

    data object ReJoin : JoinReason() {
        override val message: String = "full-rejoin"
    }

    data object Migrate : JoinReason() {
        override val message: String = "migrate"
    }

    data object Unknown : JoinReason() {
        override val message: String = "unknown"
    }

    data class Custom(
        override val message: String,
    ) : JoinReason()
}
