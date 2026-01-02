/*
 * Copyright (c) 2014-2025 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core.notifications.internal.service

internal enum class CallServiceHandleNotificationResult {
    /**
     * The notification was handled successfully and the service should start as usual,
     * initializing its state and resources.
     */
    START,

    /**
     * The notification was handled, but it did not change the service's state.
     * The service should continue running but should not re-initialize its components.
     */
    START_NO_CHANGE,

    /**
     * The notification could not be handled properly.
     * The service should stop and request the system to redeliver the intent at a later time.
     */
    REDELIVER,
}
