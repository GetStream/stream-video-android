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

package io.getstream.video.android.core.notifications.internal.telecom.connection

import android.content.Context
import android.telecom.Connection

/**
 * TODO Rahul, how to use it WIP
 */
class ErrorTelecomConnection(
    val context: Context,
) : Connection() {

    override fun onAnswer() {
        super.onAnswer()
    }

    override fun onReject() {
        super.onReject()
    }

    override fun onAnswer(videoState: Int) {
        super.onAnswer(videoState)
    }

    override fun onAbort() {
        super.onAbort()
    }

    override fun onDisconnect() {
        super.onDisconnect()
    }
}
