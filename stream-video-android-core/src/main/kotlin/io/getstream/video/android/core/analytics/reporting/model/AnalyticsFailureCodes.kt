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

internal enum class AnalyticsFailureCodes(val code: String, val message: String) {
    CLIENT_ABORTED("CLIENT_ABORTED", "Aborted: user left during retry"),
    BACKEND_LEAVE("BACKEND_LEAVE", "Aborted: backend ended call during connect"),
    NETWORK_OFFLINE("NETWORK_OFFLINE", "Device offline"),
    ICE_GATHERING_FAILED("ICE_GATHERING_FAILED", "ICE gathering failed"),
    ICE_CONNECTIVITY_FAILED("ICE_CONNECTIVITY_FAILED", "ICE connectivity failed"),
    REQUEST_TIMEOUT("REQUEST_TIMEOUT", "Device offline"),
    SFU_REQUEST_TIMEOUT("REQUEST_TIMEOUT", "SFU connection timed out"),
}
