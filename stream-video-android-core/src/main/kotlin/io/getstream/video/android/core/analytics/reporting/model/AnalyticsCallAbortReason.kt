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

package io.getstream.video.android.core.analytics.reporting.model

internal enum class AnalyticsCallAbortReason(val code: String) {
    CLIENT_ABORTED("CLIENT_ABORTED"),
    BACKEND_LEAVE("BACKEND_LEAVE"),
    NETWORK_OFFLINE("NETWORK_OFFLINE"),
    RETRY_EXHAUSTED("RETRY_EXHAUSTED"),
    SDK_DRIVEN_ANDROID("SDK_DRIVEN_ANDROID"),
    REQUEST_TIMEOUT("REQUEST_TIMEOUT"),
    SFU_ERROR("SFU_ERROR"),
    SERVER_ERROR("SERVER_ERROR"),
    CUSTOM("CUSTOM"),
}
