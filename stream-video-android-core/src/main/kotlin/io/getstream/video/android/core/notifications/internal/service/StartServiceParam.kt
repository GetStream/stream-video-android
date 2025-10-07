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

package io.getstream.video.android.core.notifications.internal.service

import io.getstream.video.android.core.Call
import io.getstream.video.android.model.StreamCallId

/**
 * Will be used when integrating Telecom Platform API
 */
private data class StartServiceParam(
    val callId: StreamCallId,
    val trigger: String,
    val callDisplayName: String? = null,
    val callServiceConfiguration: CallServiceConfig = DefaultCallConfigurations.default,
)

private data class StopServiceParam(
    val call: Call? = null,
    val callServiceConfiguration: CallServiceConfig = DefaultCallConfigurations.default,
)

private sealed class StopForegroundServiceSource(val source: String) {
    data object CallAccept : StopForegroundServiceSource("accept the call")
    data object SetActiveCall : StopForegroundServiceSource("set active call")
    data object RemoveActiveCall : StopForegroundServiceSource("remove active call")
    data object RemoveRingingCall : StopForegroundServiceSource("remove ringing call")
}
