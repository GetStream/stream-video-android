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

package io.getstream.video.android.core.telecom.bridge.telecomtostream

import android.telecom.DisconnectCause
import io.getstream.video.android.core.telecom.TelecomCall
import kotlin.getValue

internal interface TelecomToStreamEventBridgeFactory {
    fun create(telecomCall: TelecomCall): TelecomToStreamEventBridge
}

internal interface TelecomToStreamEventBridge {
    suspend fun onAnswer(callType: Int)
    suspend fun onDisconnect(cause: DisconnectCause)
    suspend fun onSetActive()
    suspend fun onSetInactive()
}
