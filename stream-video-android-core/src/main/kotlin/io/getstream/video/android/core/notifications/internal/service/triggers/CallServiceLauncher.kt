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

package io.getstream.video.android.core.notifications.internal.service.triggers

import android.app.Notification
import android.content.Context
import io.getstream.video.android.core.notifications.internal.service.CallService
import io.getstream.video.android.core.notifications.internal.service.CallServiceConfig
import io.getstream.video.android.core.notifications.internal.service.DefaultCallConfigurations
import io.getstream.video.android.model.StreamCallId

internal interface CallServiceLauncher {

    fun showIncomingCall(
        context: Context,
        callId: StreamCallId,
        callDisplayName: String?,
        callServiceConfiguration: CallServiceConfig = DefaultCallConfigurations.default,
        payload: Map<String, Any?>,
        notification: Notification?,
    )

    fun removeIncomingCall(
        context: Context,
        callId: StreamCallId,
        config: CallServiceConfig = DefaultCallConfigurations.default,
    )

    fun getServiceClass(): Class<*> {
        return CallService::class.java
    }
}
