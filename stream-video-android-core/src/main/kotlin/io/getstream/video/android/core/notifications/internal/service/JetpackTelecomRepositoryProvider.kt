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

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallsManager
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.notifications.internal.telecom.IncomingCallTelecomAction
import io.getstream.video.android.core.notifications.internal.telecom.jetpack.JetpackTelecomRepository
import io.getstream.video.android.model.StreamCallId

internal class JetpackTelecomRepositoryProvider(private val context: Context) {

    @RequiresApi(Build.VERSION_CODES.O)
    fun get(callId: StreamCallId): JetpackTelecomRepository {
        val callsManager = CallsManager(context).apply {
            registerAppWithTelecom(
                capabilities = CallsManager.CAPABILITY_SUPPORTS_CALL_STREAMING and
                    CallsManager.CAPABILITY_SUPPORTS_VIDEO_CALLING,
            )
        }

        val streamVideo = StreamVideo.instance()
        val incomingCallTelecomAction =
            IncomingCallTelecomAction(streamVideo)
        return JetpackTelecomRepository(callsManager, callId, incomingCallTelecomAction)
    }
}
