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

package io.getstream.video.android.core.notifications.internal.service.models

/** Identifies the route selected by the SDK to manage a call. */
internal enum class ServiceRoute {

    /** A route has not been selected yet. */
    UNDECIDED,

    /** The call is managed through CallService. */
    LEGACY_CALL_SERVICE,

    /** The call is managed through Telecom. */
    TELECOM,
}
